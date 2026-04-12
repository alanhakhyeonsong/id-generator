package me.ramos.idgenerator.web.dto

import me.ramos.idgenerator.web.dto.enums.ResponseTypeCodeKind

/**
 * API 공통 응답 래퍼.
 *
 * 모든 REST API 응답은 이 형식을 따른다.
 * [header]에 성공/실패 여부와 결과 코드를, [data]에 실제 응답 데이터를 담는다.
 *
 * @param T 응답 데이터 타입
 * @property header 응답 헤더 (성공/실패, 코드, 메시지)
 * @property data 응답 데이터
 * @author HakHyeon Song
 */
data class CommonResponse<T>(
    val header: Header,
    val data: T? = null,
) {
    companion object {
        fun ok(): CommonResponse<Unit?> = ok(null)

        fun <U> ok(data: U?): CommonResponse<U?> {
            val header = Header(
                isSuccessful = true,
                resultCode = ResponseTypeCodeKind.SUCCESS.resultCode,
                message = ResponseTypeCodeKind.SUCCESS.message,
            )
            return CommonResponse(header, data)
        }

        fun error(resultCode: Int, message: String, detail: String? = null): CommonResponse<Unit?> {
            val header = Header(
                isSuccessful = false,
                resultCode = resultCode,
                message = message,
                detail = detail,
            )
            return CommonResponse(header, null)
        }
    }
}
