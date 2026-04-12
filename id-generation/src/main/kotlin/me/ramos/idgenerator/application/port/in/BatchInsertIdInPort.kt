package me.ramos.idgenerator.application.port.`in`

/**
 * Base33 랜덤 ID 값을 사전 배치 생성하는 유스케이스 입력 포트.
 *
 * 100,000건의 4자리 Base33 값을 DB에 사전 적재한다.
 *
 * @author HakHyeon Song
 */
fun interface BatchInsertIdInPort {
    fun execute()
}
