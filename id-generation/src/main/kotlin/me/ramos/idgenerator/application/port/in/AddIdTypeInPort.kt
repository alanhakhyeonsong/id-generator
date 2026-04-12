package me.ramos.idgenerator.application.port.`in`

/**
 * 새로운 ID 생성 타입을 등록하는 유스케이스 입력 포트.
 *
 * 타입별 시퀀스 관리 엔티티를 생성하고 캐시에 적재한다.
 * 서로소(coprime) 기반 증분값을 랜덤으로 설정하여 ID 순서를 예측 불가능하게 한다.
 *
 * @author HakHyeon Song
 */
fun interface AddIdTypeInPort {
    fun execute(type: String)
}
