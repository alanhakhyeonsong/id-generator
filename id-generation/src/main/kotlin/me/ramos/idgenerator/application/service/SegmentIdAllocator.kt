package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.application.exception.IdTypeNotFoundException
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import me.ramos.idgenerator.common.component.lock.DistributedLockManager
import me.ramos.idgenerator.common.component.lock.LockCallback
import me.ramos.idgenerator.domain.model.UsedIdTypeInfo
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * ID 블록(Segment) 기반 시퀀스 할당기.
 *
 * Pod별로 ID 블록을 사전 확보하고, 블록 내에서는 분산 락 없이
 * [AtomicInteger][java.util.concurrent.atomic.AtomicInteger] 기반으로 시퀀스를 채번한다.
 * 블록이 소진될 때만 분산 락을 획득하여 새 블록을 할당한다.
 *
 * 이를 통해 락 빈도를 매 요청(1:1)에서 [BLOCK_SIZE]:1로 대폭 감소시킨다.
 *
 * @author HakHyeon Song
 */
@Component
class SegmentIdAllocator(
    private val distributedLockManager: DistributedLockManager,
    private val cacheOutPort: CacheIdGeneratorOutPort,
    private val loadUsedIdOutPort: LoadUsedIdOutPort,
    private val saveUsedIdOutPort: SaveUsedIdOutPort,
) {
    private val segments = ConcurrentHashMap<String, IdSegment>()

    /**
     * 지정 타입의 다음 시퀀스를 반환한다.
     *
     * 현재 블록에 잔여 시퀀스가 있으면 즉시 반환 (락 없음).
     * 블록이 소진되면 분산 락을 획득하여 새 블록을 할당한다.
     */
    fun nextSequence(type: String): Long {
        while (true) {
            val segment = segments[type]
            if (segment != null) {
                val seq = segment.next()
                if (seq != null) return seq
            }
            synchronized(type.intern()) {
                val current = segments[type]
                if (current == null || current === segment) {
                    log.info { "블록 할당 시작: type=$type" }
                    segments[type] = allocateNewSegment(type)
                }
            }
        }
    }

    private fun allocateNewSegment(type: String): IdSegment {
        return distributedLockManager.executeWithLock(
            lockName = "ID-SEGMENT:$type",
            waitTime = 10L,
            leaseTime = 5L,
            timeUnit = TimeUnit.SECONDS,
            callback = LockCallback { doAllocateSegment(type) },
        )
    }

    private fun doAllocateSegment(type: String): IdSegment {
        val entity = cacheOutPort.getOrLoad(type) {
            loadUsedIdOutPort.findByType(type)
                ?: throw IdTypeNotFoundException(type)
        }

        val typeInfo = UsedIdTypeInfo(entity)
        val capacity = entity.capacity.toLong()

        if (typeInfo.isExhausted()) {
            typeInfo.updateNextRange()
            saveUsedIdOutPort.advanceToNextRange(
                type = type,
                seqIncrement = entity.seqIncrement,
                endSeq = entity.endSeq,
            )
        }

        val remaining = capacity - entity.count
        val blockSize = minOf(BLOCK_SIZE, remaining).toInt()

        val sequences = LongArray(blockSize)
        for (i in 0 until blockSize) {
            sequences[i] = typeInfo.nextSequence()
        }

        saveUsedIdOutPort.updateSequence(type, entity.currentSeq, entity.count)
        cacheOutPort.put(type, entity)

        log.info { "블록 할당 완료: type=$type, blockSize=$blockSize, remaining=${remaining - blockSize}" }

        return IdSegment(sequences)
    }

    companion object {
        const val BLOCK_SIZE = 1000L
    }
}
