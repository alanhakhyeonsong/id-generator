package me.ramos.idgenerator.web.dto

import me.ramos.idgenerator.web.dto.enums.ResponseTypeCodeKind
import org.springframework.data.domain.Page

/**
 * 페이지네이션 API 공통 응답 래퍼.
 *
 * @param T 응답 데이터 타입
 * @property header 응답 헤더
 * @property data 페이지 데이터 목록
 * @property pageInfo 페이지 정보
 * @author HakHyeon Song
 */
data class CommonPageResponse<T>(
    val header: Header,
    val data: List<T>,
    val pageInfo: PageInfo,
) {
    companion object {
        fun <U> ok(page: Page<U>): CommonPageResponse<U> {
            val header = Header(
                isSuccessful = true,
                resultCode = ResponseTypeCodeKind.SUCCESS.resultCode,
                message = ResponseTypeCodeKind.SUCCESS.message,
            )
            val pageInfo = PageInfo(
                page = page.number,
                size = page.size,
                totalPage = page.totalPages,
                totalSize = page.totalElements,
            )
            return CommonPageResponse(header, page.content, pageInfo)
        }
    }
}

data class PageInfo(
    val page: Int,
    val size: Int,
    val totalPage: Int,
    val totalSize: Long,
)
