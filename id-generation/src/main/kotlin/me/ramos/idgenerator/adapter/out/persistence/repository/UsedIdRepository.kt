package me.ramos.idgenerator.adapter.out.persistence.repository

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * UsedId JPA 리포지토리.
 *
 * 기본 CRUD는 [JpaRepository], 커스텀 쿼리는 [UsedIdRepositoryCustom] (QueryDSL)으로 처리한다.
 *
 * @author HakHyeon Song
 */
interface UsedIdRepository : JpaRepository<UsedIdJpaEntity, Long>, UsedIdRepositoryCustom {

    fun findByType(type: String): UsedIdJpaEntity?
}
