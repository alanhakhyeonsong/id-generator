package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.`in`.AddIdTypeInPort
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import org.springframework.stereotype.Service
import kotlin.math.abs

private val log = KotlinLogging.logger {}

@Service
class AddIdTypeUseCase(
    private val loadUsedIdOutPort: LoadUsedIdOutPort,
    private val saveUsedIdOutPort: SaveUsedIdOutPort,
    private val cacheOutPort: CacheIdGeneratorOutPort,
) : AddIdTypeInPort {

    override fun execute(type: String) {
        val existing = loadUsedIdOutPort.findByType(type)
        if (existing != null) {
            log.warn { "이미 존재하는 타입: type=$type" }
            return
        }

        val capacity = UsedIdJpaEntity.DEFAULT_CAPACITY.toLong()
        val coprimeIncrement = generateCoprime(capacity)
        val endSeq = abs(System.nanoTime()) % capacity

        val entity = UsedIdJpaEntity.create(
            type = type,
            seqIncrement = coprimeIncrement,
            endSeq = endSeq,
        )

        val saved = saveUsedIdOutPort.save(entity)
        cacheOutPort.put(type, saved)
        log.info { "새 ID 타입 추가: type=$type, increment=$coprimeIncrement" }
    }

    private fun generateCoprime(capacity: Long): Long {
        val candidates = (2..capacity / 2).filter { gcd(it, capacity) == 1L }
        return candidates.random()
    }

    private fun gcd(a: Long, b: Long): Long {
        var x = a
        var y = b
        while (y != 0L) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }
}
