package me.ramos.idgenerator.application.port.`in`

/**
 * 고유 ID를 생성하는 유스케이스 입력 포트.
 *
 * 분산 락 기반으로 지정된 타입에 대해 고유한 ID를 생성한다.
 * 결과 형식: `{type}-{base33RandomValue}`
 *
 * @author HakHyeon Song
 */
fun interface GenerateIdInPort {
    fun execute(type: String): String
}
