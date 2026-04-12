package me.ramos.idgenerator.application.exception

import me.ramos.idgenerator.dto.ResponseTypeCodeInterface

/**
 * ID 생성 도메인 에러 코드.
 *
 * @author HakHyeon Song
 */
enum class IdGenerationExceptionCode(
    override val httpStatus: Int,
    override val resultCode: Int,
    override val message: String,
) : ResponseTypeCodeInterface {

    ID_EXHAUSTED(409, 1001, "현재 범위의 ID가 모두 소진되었습니다."),
    ID_TYPE_NOT_FOUND(404, 1002, "요청한 ID 타입이 존재하지 않습니다."),
    ID_TYPE_ALREADY_EXISTS(409, 1003, "이미 존재하는 ID 타입입니다."),
    LOCK_ACQUISITION_FAILED(503, 1004, "분산 락 획득에 실패했습니다."),
    ;
}
