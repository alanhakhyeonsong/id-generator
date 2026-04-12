package me.ramos.idgenerator.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

@Entity
@Table(name = "used_id")
class UsedIdJpaEntity protected constructor(
    type: String,
    capacity: Int,
    currentSeq: Long,
    endSeq: Long,
    seqIncrement: Long,
    seqRange: Int,
    count: Long,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(nullable = false, length = 10, unique = true)
    @Comment("ID 타입 접두사")
    var type: String = type
        protected set

    @Column(nullable = false)
    @Comment("범위당 용량 (기본 100,000)")
    var capacity: Int = capacity
        protected set

    @Column(nullable = false)
    @Comment("현재 시퀀스 위치")
    var currentSeq: Long = currentSeq
        protected set

    @Column(nullable = false)
    @Comment("종료 시퀀스 (참조용)")
    var endSeq: Long = endSeq
        protected set

    @Column(nullable = false)
    @Comment("서로소 증분값")
    var seqIncrement: Long = seqIncrement
        protected set

    @Column(nullable = false)
    @Comment("현재 범위 인덱스 (0, 1, 2, ...)")
    var seqRange: Int = seqRange
        protected set

    @Column(nullable = false)
    @Comment("현재 범위에서 생성된 ID 수")
    var count: Long = count
        protected set

    fun updateSequence(newSeq: Long, newCount: Long) {
        this.currentSeq = newSeq
        this.count = newCount
    }

    fun advanceToNextRange(newIncrement: Long, newEndSeq: Long) {
        this.seqRange += 1
        this.currentSeq = 0
        this.count = 0
        this.seqIncrement = newIncrement
        this.endSeq = newEndSeq
    }

    companion object {
        const val DEFAULT_CAPACITY = 100_000

        fun create(
            type: String,
            capacity: Int = DEFAULT_CAPACITY,
            seqIncrement: Long,
            endSeq: Long,
        ): UsedIdJpaEntity = UsedIdJpaEntity(
            type = type,
            capacity = capacity,
            currentSeq = 0,
            endSeq = endSeq,
            seqIncrement = seqIncrement,
            seqRange = 0,
            count = 0,
        )
    }
}
