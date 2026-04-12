package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.`in`.BatchInsertIdInPort
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import me.ramos.idgenerator.domain.util.IdGenerationUtil
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class BatchInsertIdUseCase(
    private val loadRandomIdOutPort: LoadRandomIdOutPort,
) : BatchInsertIdInPort {

    override fun execute() {
        val capacity = UsedIdJpaEntity.DEFAULT_CAPACITY
        val entities = (1..capacity).map { seq ->
            val base33Value = IdGenerationUtil.toBase33Padded(seq.toLong())
            RandomIdGeneratorJpaEntity.create(base33Value)
        }

        loadRandomIdOutPort.saveAll(entities)
        log.info { "배치 ID 삽입 완료: count=$capacity" }
    }
}
