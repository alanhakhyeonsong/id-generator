package me.ramos.idgenerator.application.service

import java.util.concurrent.atomic.AtomicInteger

/**
 * 사전 할당된 ID 시퀀스 블록.
 *
 * Pod 로컬에서 [AtomicInteger] 기반으로 시퀀스를 순차 소비한다.
 * 블록이 소진되면 새 블록을 할당받아야 한다.
 *
 * @author HakHyeon Song
 */
class IdSegment(
    private val sequences: LongArray,
) {
    private val cursor = AtomicInteger(0)

    fun next(): Long? {
        val index = cursor.getAndIncrement()
        return if (index < sequences.size) sequences[index] else null
    }

    fun remaining(): Int = (sequences.size - cursor.get()).coerceAtLeast(0)
}
