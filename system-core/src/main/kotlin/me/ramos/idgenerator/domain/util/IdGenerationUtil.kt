package me.ramos.idgenerator.domain.util

/**
 * Base33 인코딩/디코딩 유틸리티.
 *
 * I, O, S, L을 제외한 33개 문자(0-9, A-Z 중 선택)를 사용하여
 * 사람이 읽기 쉬운 짧은 ID 문자열을 생성한다.
 *
 * @author HakHyeon Song
 */
object IdGenerationUtil {

    private const val BASE = 33
    private const val BASE_CHARS = "0123456789ABCDFGHJKLMNPQRSTUVWXYZ"

    fun toBase33(decimal: Long): String {
        require(decimal >= 0) { "음수는 변환할 수 없습니다: $decimal" }
        if (decimal == 0L) return "0"

        val sb = StringBuilder()
        var value = decimal
        while (value > 0) {
            sb.append(BASE_CHARS[(value % BASE).toInt()])
            value /= BASE
        }
        return sb.reverse().toString()
    }

    fun toDecimal(base33: String): Long {
        require(base33.isNotBlank()) { "빈 문자열은 변환할 수 없습니다" }

        var result = 0L
        for (char in base33) {
            val index = BASE_CHARS.indexOf(char)
            require(index >= 0) { "유효하지 않은 Base33 문자: $char" }
            result = result * BASE + index
        }
        return result
    }

    fun toBase33Padded(decimal: Long, length: Int = 4): String {
        return toBase33(decimal).padStart(length, '0')
    }
}
