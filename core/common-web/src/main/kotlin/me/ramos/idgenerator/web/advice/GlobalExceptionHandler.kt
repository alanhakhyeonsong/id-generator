package me.ramos.idgenerator.web.advice

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.exception.BaseRuntimeException
import me.ramos.idgenerator.web.dto.CommonResponse
import me.ramos.idgenerator.web.dto.Header
import me.ramos.idgenerator.web.dto.enums.ResponseTypeCodeKind
import me.ramos.idgenerator.web.dto.enums.ValidationCodeKind
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

private val log = KotlinLogging.logger {}

/**
 * 전역 예외 처리기.
 *
 * 모든 REST API에서 발생하는 예외를 [CommonResponse] 형식으로 변환한다.
 * 도메인별 예외는 [BaseRuntimeException]을 상속하여 에러 코드를 매핑한다.
 *
 * @author HakHyeon Song
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BaseRuntimeException::class)
    fun handleBaseRuntimeException(ex: BaseRuntimeException): ResponseEntity<CommonResponse<Unit?>> {
        log.warn { "[BaseRuntimeException] code=${ex.exceptionCode.resultCode}, message=${ex.loggingMessage}" }
        return buildResponse(
            status = HttpStatus.valueOf(ex.exceptionCode.httpStatus),
            resultCode = ex.exceptionCode.resultCode,
            message = ex.exceptionCode.message,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<CommonResponse<Unit?>> {
        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        val code = ValidationCodeKind.fromConstraint(firstError?.code)
        val detail = firstError?.let { "${it.field}: ${it.defaultMessage}" }
        log.warn { "[ValidationError] $detail" }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            resultCode = code.resultCode,
            message = ResponseTypeCodeKind.VALIDATION_ERROR.message,
            detail = detail,
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(ex: MissingServletRequestParameterException): ResponseEntity<CommonResponse<Unit?>> {
        log.warn { "[MissingParameter] ${ex.parameterName}" }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            resultCode = ResponseTypeCodeKind.MISSING_PARAMETER.resultCode,
            message = ResponseTypeCodeKind.MISSING_PARAMETER.message,
            detail = "파라미터: ${ex.parameterName} (${ex.parameterType})",
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<CommonResponse<Unit?>> {
        log.warn { "[TypeMismatch] ${ex.name}: ${ex.value}" }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            resultCode = ResponseTypeCodeKind.TYPE_MISMATCH.resultCode,
            message = ResponseTypeCodeKind.TYPE_MISMATCH.message,
            detail = "파라미터: ${ex.name}, 값: ${ex.value}",
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<CommonResponse<Unit?>> {
        log.warn { "[MessageNotReadable] ${ex.message}" }
        return buildResponse(
            status = HttpStatus.BAD_REQUEST,
            resultCode = ResponseTypeCodeKind.MESSAGE_NOT_READABLE.resultCode,
            message = ResponseTypeCodeKind.MESSAGE_NOT_READABLE.message,
        )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(ex: NoResourceFoundException): ResponseEntity<CommonResponse<Unit?>> {
        return buildResponse(
            status = HttpStatus.NOT_FOUND,
            resultCode = ResponseTypeCodeKind.NOT_FOUND.resultCode,
            message = ResponseTypeCodeKind.NOT_FOUND.message,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(ex: HttpRequestMethodNotSupportedException): ResponseEntity<CommonResponse<Unit?>> {
        return buildResponse(
            status = HttpStatus.METHOD_NOT_ALLOWED,
            resultCode = ResponseTypeCodeKind.METHOD_NOT_ALLOWED.resultCode,
            message = ResponseTypeCodeKind.METHOD_NOT_ALLOWED.message,
            detail = "메서드: ${ex.method}",
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<CommonResponse<Unit?>> {
        log.error(ex) { "[UnexpectedException] ${ex.message}" }
        return buildResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            resultCode = ResponseTypeCodeKind.INTERNAL_SERVER_ERROR.resultCode,
            message = ResponseTypeCodeKind.INTERNAL_SERVER_ERROR.message,
        )
    }

    private fun buildResponse(
        status: HttpStatus,
        resultCode: Int,
        message: String,
        detail: String? = null,
    ): ResponseEntity<CommonResponse<Unit?>> {
        val header = Header(
            isSuccessful = false,
            resultCode = resultCode,
            message = message,
            detail = detail,
        )
        return ResponseEntity.status(status).body(CommonResponse(header, null))
    }
}
