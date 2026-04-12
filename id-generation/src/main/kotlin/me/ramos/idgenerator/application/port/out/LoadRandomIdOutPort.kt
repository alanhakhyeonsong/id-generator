package me.ramos.idgenerator.application.port.out

import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity

/**
 * 사전 생성된 랜덤 ID 값을 조회/저장하는 출력 포트.
 *
 * @author HakHyeon Song
 */
interface LoadRandomIdOutPort {
    fun findById(seq: Long): RandomIdGeneratorJpaEntity?
    fun saveAll(entities: List<RandomIdGeneratorJpaEntity>): List<RandomIdGeneratorJpaEntity>
    fun getMaxSequence(): Long?
}
