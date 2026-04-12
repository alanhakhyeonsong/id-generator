package me.ramos.idgenerator.adapter.out.persistence.repository

import me.ramos.idgenerator.adapter.out.persistence.entity.RandomIdGeneratorJpaEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RandomIdGeneratorRepository : JpaRepository<RandomIdGeneratorJpaEntity, Long>
