package me.ramos.idgenerator.exception

import me.ramos.idgenerator.dto.ResponseTypeCodeInterface

/**
 * 모든 도메인별 비즈니스 예외의 기반 클래스.
 *
 * [exceptionCode]를 통해 HTTP 상태, 결과 코드, 메시지를 일관되게 매핑한다.
 * [loggingMessage]는 내부 로깅 용도이며 API 응답에 노출되지 않는다.
 *
 * @property exceptionCode 에러 코드 인터페이스 구현체
 * @property loggingMessage 내부 로깅용 상세 메시지
 * @author HakHyeon Song
 */
open class BaseRuntimeException(
    val exceptionCode: ResponseTypeCodeInterface,
    val loggingMessage: String? = null,
    cause: Throwable? = null,
) : RuntimeException(loggingMessage, cause)
