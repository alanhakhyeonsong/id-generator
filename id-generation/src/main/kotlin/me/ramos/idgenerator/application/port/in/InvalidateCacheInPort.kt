package me.ramos.idgenerator.application.port.`in`

/**
 * ID 생성기 캐시를 무효화하는 유스케이스 입력 포트.
 *
 * Valkey에 캐시된 시퀀스 상태를 삭제하여 다음 요청 시 DB에서 최신 상태를 로드하도록 한다.
 * DB 수동 수정 후 캐시 동기화가 필요할 때 사용한다.
 *
 * @author HakHyeon Song
 */
interface InvalidateCacheInPort {
    fun execute(type: String?)
}
