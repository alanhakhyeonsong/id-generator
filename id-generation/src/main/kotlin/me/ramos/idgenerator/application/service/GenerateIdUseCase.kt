package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.application.exception.IdExhaustedException
import me.ramos.idgenerator.application.port.`in`.GenerateIdInPort
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import me.ramos.idgenerator.common.component.lock.DistributedLockManager
import me.ramos.idgenerator.common.component.lock.LockCallback
import me.ramos.idgenerator.domain.model.UsedIdTypeInfo
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class GenerateIdUseCase(
    private val distributedLockManager: DistributedLockManager,
    private val cacheOutPort: CacheIdGeneratorOutPort,
    private val loadUsedIdOutPort: LoadUsedIdOutPort,
    private val saveUsedIdOutPort: SaveUsedIdOutPort,
    private val loadRandomIdOutPort: LoadRandomIdOutPort,
) : GenerateIdInPort {

    override fun execute(type: String): String {
        return distributedLockManager.executeWithLock(
            lockName = "ID-GENERATOR:$type",
            waitTime = 5L,
            leaseTime = 3L,
            timeUnit = TimeUnit.SECONDS,
            callback = LockCallback { doGenerateId(type) },
        )
    }

    private fun doGenerateId(type: String): String {
        val entity = cacheOutPort.getOrLoad(type) {
            loadUsedIdOutPort.findByTypeWithLock(type)
                ?: throw IllegalStateException("ID 타입이 존재하지 않습니다: type=$type")
        }

        val typeInfo = UsedIdTypeInfo(entity)

        if (typeInfo.isExhausted()) {
            typeInfo.updateNextRange()
            saveUsedIdOutPort.advanceToNextRange(
                type = type,
                seqIncrement = entity.seqIncrement,
                endSeq = entity.endSeq,
            )
        }

        val seq = typeInfo.nextSequence()
        saveUsedIdOutPort.updateSequence(type, entity.currentSeq, entity.count)
        cacheOutPort.put(type, entity)

        val randomId = loadRandomIdOutPort.findById(seq)
            ?: throw IdExhaustedException(type)

        return "$type-${randomId.randomNo}"
    }
}
