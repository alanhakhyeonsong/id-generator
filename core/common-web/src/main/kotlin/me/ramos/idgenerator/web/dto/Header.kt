package me.ramos.idgenerator.web.dto

/**
 * API 공통 응답 헤더.
 *
 * @property isSuccessful 요청 성공 여부
 * @property resultCode 애플리케이션 결과 코드
 * @property message 결과 메시지
 * @property detail 추가 상세 정보 (디버그 등)
 * @author HakHyeon Song
 */
data class Header(
    val isSuccessful: Boolean,
    val resultCode: Int,
    val message: String = "",
    val detail: String? = null,
)
