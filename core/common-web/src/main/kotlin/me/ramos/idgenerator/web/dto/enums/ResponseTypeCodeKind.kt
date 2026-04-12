package me.ramos.idgenerator.web.dto.enums

import me.ramos.idgenerator.dto.ResponseTypeCodeInterface
import org.springframework.http.HttpStatus

/**
 * 시스템 공통 응답 코드.
 *
 * 도메인별 에러 코드는 각 모듈에서 [ResponseTypeCodeInterface]를 구현하여 별도 정의한다.
 *
 * @author HakHyeon Song
 */
enum class ResponseTypeCodeKind(
    override val httpStatus: Int,
    override val resultCode: Int,
    override val message: String,
) : ResponseTypeCodeInterface {

    // 2xx
    SUCCESS(HttpStatus.OK.value(), 200, "요청이 성공적으로 처리되었습니다."),

    // 4xx
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), 400, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND.value(), 404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED.value(), 405, "허용되지 않은 HTTP 메서드입니다."),
    CONFLICT(HttpStatus.CONFLICT.value(), 409, "리소스 충돌이 발생했습니다."),

    // 4xx - Validation
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST.value(), 422, "입력값 검증에 실패했습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST.value(), 423, "필수 파라미터가 누락되었습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST.value(), 424, "파라미터 타입이 올바르지 않습니다."),
    MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST.value(), 425, "요청 본문을 읽을 수 없습니다."),

    // 5xx
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), 500, "내부 서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE.value(), 503, "서비스를 일시적으로 사용할 수 없습니다."),
    ;
}
