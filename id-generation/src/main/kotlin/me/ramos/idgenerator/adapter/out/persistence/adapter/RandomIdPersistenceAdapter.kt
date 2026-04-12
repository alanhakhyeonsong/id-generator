package me.ramos.idgenerator.adapter.out.persistence.adapter

import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import me.ramos.idgenerator.adapter.out.persistence.repository.RandomIdGeneratorRepository
import me.ramos.idgenerator.application.port.out.LoadRandomIdOutPort
import org.springframework.stereotype.Component

@Component
class RandomIdPersistenceAdapter(
    private val randomIdGeneratorRepository: RandomIdGeneratorRepository,
) : LoadRandomIdOutPort {

    override fun findById(seq: Long): RandomIdGeneratorJpaEntity? =
        randomIdGeneratorRepository.findById(seq).orElse(null)

    override fun saveAll(entities: List<RandomIdGeneratorJpaEntity>): List<RandomIdGeneratorJpaEntity> =
        randomIdGeneratorRepository.saveAll(entities)
}
