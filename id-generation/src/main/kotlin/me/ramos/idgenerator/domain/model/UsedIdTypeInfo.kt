package me.ramos.idgenerator.domain.model

import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import kotlin.math.abs

/**
 * ID 타입별 시퀀스 생성 로직을 관리하는 도메인 모델.
 *
 * 선형 합동 생성기(Linear Congruential Generator) 알고리즘을 사용하여
 * capacity 범위 내에서 중복 없는 시퀀스를 생성한다.
 * 서로소(coprime) 증분값을 사용해 전체 범위를 순회한다.
 *
 * @author HakHyeon Song
 */
class UsedIdTypeInfo(
    val entity: UsedIdJpaEntity,
) {
    private val coprimes: List<Long> = calculateCoprimes(entity.capacity.toLong())

    fun nextSequence(): Long {
        val capacity = entity.capacity.toLong()
        val start = entity.currentSeq
        val increment = entity.seqIncrement
        val next = (capacity + (start - 1 + increment)) % capacity + 1
        entity.updateSequence(next, entity.count + 1)
        return next + (entity.seqRange.toLong() * capacity)
    }

    fun isExhausted(): Boolean {
        return entity.count >= entity.capacity
    }

    fun updateNextRange() {
        val newIncrement = coprimes.random()
        val newEndSeq = abs(System.nanoTime()) % entity.capacity
        entity.advanceToNextRange(newIncrement, newEndSeq)
    }

    companion object {
        private fun calculateCoprimes(capacity: Long): List<Long> {
            return (2..capacity / 2)
                .filter { gcd(it, capacity) == 1L }
                .toList()
        }

        private fun gcd(a: Long, b: Long): Long {
            var x = a
            var y = b
            while (y != 0L) {
                val temp = y
                y = x % y
                x = temp
            }
            return x
        }
    }
}
