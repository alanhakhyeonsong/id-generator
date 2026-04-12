package me.ramos.idgenerator.common.component.lock

/**
 * 분산 락 내에서 실행할 비즈니스 로직을 정의하는 함수형 인터페이스.
 *
 * @param T 실행 결과 타입
 * @author HakHyeon Song
 */
fun interface LockCallback<T> {
    fun execute(): T
}
