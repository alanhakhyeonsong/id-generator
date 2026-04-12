package me.ramos.idgenerator.web.filter

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * HTTP 요청 로깅 및 Request ID 추적 필터.
 *
 * 모든 요청에 대해 Request ID를 MDC에 설정하고,
 * 요청 시작/종료 시 메서드, 경로, 상태 코드, 소요 시간을 로깅한다.
 *
 * @author HakHyeon Song
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
class LoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = resolveRequestId(request)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        MDC.put(MDC_REQUEST_ID_KEY, requestId)

        val startNs = System.nanoTime()
        val method = request.method
        val path = request.requestURI
        var error: Exception? = null

        try {
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            error = e
            log.error(e) { "http_request_error requestId=$requestId method=$method path=$path" }
        } finally {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            log.info {
                "http_request_end requestId=$requestId method=$method path=$path status=${response.status} elapsedMs=$elapsedMs"
            }
            MDC.remove(MDC_REQUEST_ID_KEY)
        }

        if (error != null) throw error
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/actuator/health") ||
            path.startsWith("/actuator/info") ||
            path.startsWith("/actuator/prometheus")
    }

    private fun resolveRequestId(request: HttpServletRequest): String {
        val headerRequestId = request.getHeader(REQUEST_ID_HEADER)
        val headerCorrelationId = request.getHeader(CORRELATION_ID_HEADER)
        return when {
            !headerRequestId.isNullOrBlank() -> headerRequestId
            !headerCorrelationId.isNullOrBlank() -> headerCorrelationId
            else -> UUID.randomUUID().toString()
        }
    }

    private companion object {
        private const val REQUEST_ID_HEADER = "X-Request-Id"
        private const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        private const val MDC_REQUEST_ID_KEY = "requestId"
    }
}
