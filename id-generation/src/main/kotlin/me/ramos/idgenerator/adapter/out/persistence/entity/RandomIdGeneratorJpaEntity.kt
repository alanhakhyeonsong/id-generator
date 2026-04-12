package me.ramos.idgenerator.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

@Entity
@Table(name = "random_id_generator")
class RandomIdGeneratorJpaEntity protected constructor(
    randomNo: String,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("자동 증분 PK")
    val idGenerationSeq: Long? = null

    @Column(nullable = false, length = 4)
    @Comment("Base33 랜덤 4자리 값")
    var randomNo: String = randomNo
        protected set

    @Column(nullable = false, updatable = false)
    @Comment("생성 시각")
    val createdDateTime: LocalDateTime = LocalDateTime.now()

    companion object {
        fun create(randomNo: String): RandomIdGeneratorJpaEntity =
            RandomIdGeneratorJpaEntity(randomNo = randomNo)
    }
}
