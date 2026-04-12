package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.`in`.BatchInsertIdInPort
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.domain.util.IdGenerationUtil
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 배치 ID 삽입 유스케이스.
 *
 * [isIdGenerationRequired] 조건을 확인하여, 현재 범위의 ID가 90% 이상 소진되었거나
 * random_id_generator 테이블에 데이터가 없을 때 100,000건의 Base33 랜덤 ID를 사전 적재한다.
 *
 * @author HakHyeon Song
 */
@Service
class BatchInsertIdUseCase(
    private val loadRandomIdOutPort: LoadRandomIdOutPort,
    private val loadUsedIdOutPort: LoadUsedIdOutPort,
) : BatchInsertIdInPort {

    override fun execute() {
        val totalRandomIdCount = loadRandomIdOutPort.getMaxSequence()

        if (!loadUsedIdOutPort.isIdGenerationRequired(totalRandomIdCount)) {
            log.info { "배치 ID 생성 불필요: totalRandomIdCount=$totalRandomIdCount" }
            return
        }

        val capacity = UsedIdJpaEntity.DEFAULT_CAPACITY
        val entities = (0 until capacity).map { seq ->
            val base33Value = IdGenerationUtil.toBase33Padded(seq.toLong())
            RandomIdGeneratorJpaEntity.create(base33Value)
        }.shuffled()

        loadRandomIdOutPort.saveAll(entities)
        log.info { "배치 ID 삽입 완료: count=$capacity, totalAfter=${(totalRandomIdCount ?: 0) + capacity}" }
    }
}
