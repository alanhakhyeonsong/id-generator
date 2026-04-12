package me.ramos.idgenerator.common.component.lock

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * 분산 락과 비즈니스 트랜잭션을 분리하기 위한 트랜잭션 실행기.
 *
 * [Propagation.REQUIRES_NEW]를 사용하여 락 획득/해제 시점과 트랜잭션 커밋 시점을 분리한다.
 * 이를 통해 트랜잭션 커밋 전에 락이 해제되는 문제를 방지한다.
 *
 * @author HakHyeon Song
 */
@Component
class TransactionExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> executeInNewTransaction(callback: LockCallback<T>): T {
        return callback.execute()
    }
}
