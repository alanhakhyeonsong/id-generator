package me.ramos.idgenerator.common.component.lock

import io.github.oshai.kotlinlogging.KotlinLogging
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * Redisson 기반 분산 락 관리자.
 *
 * 락 획득 → 새 트랜잭션에서 콜백 실행 → 락 해제 흐름을 보장한다.
 * 락 획득/해제와 비즈니스 트랜잭션을 분리하기 위해 [TransactionExecutor]를 사용한다.
 *
 * @author HakHyeon Song
 */
@Component
class DistributedLockManager(
    private val redissonClient: RedissonClient,
    private val transactionExecutor: TransactionExecutor,
) {

    fun <T> executeWithLock(
        lockName: String,
        waitTime: Long,
        leaseTime: Long,
        timeUnit: TimeUnit,
        callback: LockCallback<T>,
    ): T {
        val lock = redissonClient.getLock(lockName)

        val acquired = try {
            lock.tryLock(waitTime, leaseTime, timeUnit)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Lock 획득 중 인터럽트 발생: lock=$lockName", e)
        }

        if (!acquired) {
            throw IllegalStateException("Lock 획득 실패: lock=$lockName, waitTime=$waitTime, leaseTime=$leaseTime")
        }

        log.debug { "Lock 획득 성공: lock=$lockName" }

        return try {
            transactionExecutor.executeInNewTransaction(callback)
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                log.debug { "Lock 해제 완료: lock=$lockName" }
            }
        }
    }
}
