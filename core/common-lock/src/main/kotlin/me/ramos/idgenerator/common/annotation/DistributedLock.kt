package me.ramos.idgenerator.common.annotation

import java.util.concurrent.TimeUnit

/**
 * 분산 락을 적용하는 메서드 레벨 어노테이션.
 *
 * Redisson 기반 분산 락을 AOP로 자동 적용한다.
 * [key]는 SpEL 표현식을 지원하며, 메서드 파라미터를 참조할 수 있다.
 *
 * @property key 락 이름 (SpEL 지원)
 * @property timeUnit 시간 단위
 * @property waitTime 락 획득 대기 시간
 * @property leaseTime 락 점유 시간
 * @author HakHyeon Song
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
    val waitTime: Long = 5L,
    val leaseTime: Long = 3L,
)
