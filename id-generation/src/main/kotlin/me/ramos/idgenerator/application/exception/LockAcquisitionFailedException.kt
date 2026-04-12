package me.ramos.idgenerator.application.exception

import me.ramos.idgenerator.exception.BaseRuntimeException

/**
 * 분산 락 획득 실패 예외.
 *
 * Valkey 장애 또는 Failover 중 분산 락을 획득하지 못한 경우 발생한다.
 * HTTP 503 Service Unavailable로 매핑되어 클라이언트의 재시도를 유도한다.
 *
 * @author HakHyeon Song
 */
class LockAcquisitionFailedException(
    type: String,
    cause: Throwable? = null,
) : BaseRuntimeException(
    exceptionCode = IdGenerationExceptionCode.LOCK_ACQUISITION_FAILED,
    loggingMessage = "분산 락 획득에 실패했습니다: type=$type",
) {
    init {
        if (cause != null) initCause(cause)
    }
}
