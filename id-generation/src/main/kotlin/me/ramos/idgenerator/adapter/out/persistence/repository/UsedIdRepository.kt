package me.ramos.idgenerator.adapter.out.persistence.repository

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UsedIdRepository : JpaRepository<UsedIdJpaEntity, Long> {

    fun findByType(type: String): UsedIdJpaEntity?

    @Modifying
    @Query(
        """
        UPDATE UsedIdJpaEntity u
        SET u.currentSeq = :currentSeq,
            u.count = :count
        WHERE u.type = :type
        """,
    )
    fun updateSequence(
        @Param("type") type: String,
        @Param("currentSeq") currentSeq: Long,
        @Param("count") count: Long,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE UsedIdJpaEntity u
        SET u.seqRange = u.seqRange + 1,
            u.currentSeq = 0,
            u.count = 0,
            u.seqIncrement = :seqIncrement,
            u.endSeq = :endSeq
        WHERE u.type = :type
        """,
    )
    fun advanceToNextRange(
        @Param("type") type: String,
        @Param("seqIncrement") seqIncrement: Long,
        @Param("endSeq") endSeq: Long,
    ): Int

    @Query(
        """
        SELECT CASE WHEN u.count >= (u.capacity * 0.9) THEN true ELSE false END
        FROM UsedIdJpaEntity u
        WHERE u.type = :type
        """,
    )
    fun isIdGenerationRequired(@Param("type") type: String): Boolean
}
