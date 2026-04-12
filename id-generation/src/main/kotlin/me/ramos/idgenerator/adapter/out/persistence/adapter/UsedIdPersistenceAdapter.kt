package me.ramos.idgenerator.adapter.out.persistence.adapter

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.repository.UsedIdRepository
import me.ramos.idgenerator.application.port.out.LoadUsedIdOutPort
import me.ramos.idgenerator.application.port.out.SaveUsedIdOutPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class UsedIdPersistenceAdapter(
    private val usedIdRepository: UsedIdRepository,
) : LoadUsedIdOutPort, SaveUsedIdOutPort {

    override fun findByType(type: String): UsedIdJpaEntity? =
        usedIdRepository.findByType(type)

    override fun isIdGenerationRequired(type: String): Boolean =
        usedIdRepository.isIdGenerationRequired(type)

    @Transactional
    override fun save(entity: UsedIdJpaEntity): UsedIdJpaEntity =
        usedIdRepository.save(entity)

    @Transactional
    override fun updateSequence(type: String, currentSeq: Long, count: Long): Int =
        usedIdRepository.updateSequence(type, currentSeq, count)

    @Transactional
    override fun advanceToNextRange(type: String, seqIncrement: Long, endSeq: Long): Int =
        usedIdRepository.advanceToNextRange(type, seqIncrement, endSeq)
}
