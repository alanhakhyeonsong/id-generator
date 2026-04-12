package me.ramos.idgenerator.application.exception

import me.ramos.idgenerator.exception.BaseRuntimeException

class IdTypeNotFoundException(
    type: String,
) : BaseRuntimeException(
    exceptionCode = IdGenerationExceptionCode.ID_TYPE_NOT_FOUND,
    loggingMessage = "ID 타입이 존재하지 않습니다: type=$type",
)
