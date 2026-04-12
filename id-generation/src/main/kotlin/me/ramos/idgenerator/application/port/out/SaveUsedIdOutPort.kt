package me.ramos.idgenerator.application.port.out

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity

/**
 * 사용된 ID 시퀀스 정보를 저장/갱신하는 출력 포트.
 *
 * @author HakHyeon Song
 */
interface SaveUsedIdOutPort {
    fun save(entity: UsedIdJpaEntity): UsedIdJpaEntity
    fun updateSequence(type: String, currentSeq: Long, count: Long): Int
    fun advanceToNextRange(type: String, seqIncrement: Long, endSeq: Long): Int
}
