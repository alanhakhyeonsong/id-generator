package me.ramos.idgenerator.adapter.out.persistence.adapter

import com.querydsl.jpa.impl.JPAQueryFactory
import me.ramos.idgenerator.adapter.out.persistence.entity.QRandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.repository.RandomIdGeneratorRepository
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import org.springframework.stereotype.Component

@Component
class RandomIdPersistenceAdapter(
    private val randomIdGeneratorRepository: RandomIdGeneratorRepository,
    private val queryFactory: JPAQueryFactory,
) : LoadRandomIdOutPort {

    override fun findById(seq: Long): RandomIdGeneratorJpaEntity? =
        randomIdGeneratorRepository.findById(seq).orElse(null)

    override fun saveAll(entities: List<RandomIdGeneratorJpaEntity>): List<RandomIdGeneratorJpaEntity> =
        randomIdGeneratorRepository.saveAll(entities)

    override fun getMaxSequence(): Long? {
        val entity = QRandomIdGeneratorJpaEntity.randomIdGeneratorJpaEntity
        return queryFactory
            .select(entity.idGenerationSeq.max())
            .from(entity)
            .fetchOne()
    }
}
