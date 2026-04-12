package me.ramos.idgenerator.application.port.out

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity

/**
 * 사용된 ID 시퀀스 정보를 조회하는 출력 포트.
 *
 * @author HakHyeon Song
 */
interface LoadUsedIdOutPort {
    fun findByType(type: String): UsedIdJpaEntity?
    fun isIdGenerationRequired(totalRandomIdCount: Long?): Boolean
}
