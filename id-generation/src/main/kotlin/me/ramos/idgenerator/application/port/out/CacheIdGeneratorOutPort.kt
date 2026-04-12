package me.ramos.idgenerator.application.port.out

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity

/**
 * ID 생성기 시퀀스 상태를 캐시(Valkey)에서 관리하는 출력 포트.
 *
 * DB 조회를 줄이기 위해 시퀀스 상태를 캐시에 유지하며,
 * 캐시 미스 시 DB에서 로드하여 캐시에 적재한다.
 *
 * @author HakHyeon Song
 */
interface CacheIdGeneratorOutPort {
    fun put(type: String, entity: UsedIdJpaEntity): UsedIdJpaEntity
    fun getOrLoad(type: String, loader: () -> UsedIdJpaEntity): UsedIdJpaEntity
    fun evict(type: String)
    fun evictAll()
}
