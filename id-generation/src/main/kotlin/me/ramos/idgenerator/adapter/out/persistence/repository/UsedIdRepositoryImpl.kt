package me.ramos.idgenerator.adapter.out.persistence.repository

import com.querydsl.jpa.impl.JPAQueryFactory
import me.ramos.idgenerator.adapter.out.persistence.entity.QUsedIdJpaEntity.usedIdJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import org.springframework.stereotype.Repository

/**
 * UsedId QueryDSL 커스텀 리포지토리 구현체.
 *
 * @author HakHyeon Song
 */
@Repository
class UsedIdRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UsedIdRepositoryCustom {

    override fun updateSequence(type: String, currentSeq: Long, count: Long): Long {
        return queryFactory
            .update(usedIdJpaEntity)
            .set(usedIdJpaEntity.currentSeq, currentSeq)
            .set(usedIdJpaEntity.count, count)
            .where(usedIdJpaEntity.type.eq(type))
            .execute()
    }

    override fun advanceToNextRange(type: String, seqIncrement: Long, endSeq: Long): Long {
        return queryFactory
            .update(usedIdJpaEntity)
            .set(usedIdJpaEntity.seqRange, usedIdJpaEntity.seqRange.add(1))
            .set(usedIdJpaEntity.currentSeq, 0L)
            .set(usedIdJpaEntity.count, 0L)
            .set(usedIdJpaEntity.seqIncrement, seqIncrement)
            .set(usedIdJpaEntity.endSeq, endSeq)
            .where(usedIdJpaEntity.type.eq(type))
            .execute()
    }

    override fun isIdGenerationRequired(totalRandomIdCount: Long?): Boolean {
        if (totalRandomIdCount == null) return true

        val capacity = UsedIdJpaEntity.DEFAULT_CAPACITY
        val threshold = (capacity * 0.9).toLong()

        val maxUsedId = queryFactory
            .selectFrom(usedIdJpaEntity)
            .orderBy(usedIdJpaEntity.seqRange.desc(), usedIdJpaEntity.count.desc())
            .fetchFirst() ?: return true

        val maxSeq = (maxUsedId.seqRange + 1).toLong() * capacity
        return totalRandomIdCount == maxSeq && maxUsedId.count >= threshold
    }
}
