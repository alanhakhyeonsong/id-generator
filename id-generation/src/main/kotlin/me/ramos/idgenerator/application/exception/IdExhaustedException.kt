package me.ramos.idgenerator.application.exception

import me.ramos.idgenerator.exception.BaseRuntimeException

class IdExhaustedException(
    type: String,
) : BaseRuntimeException(
    exceptionCode = IdGenerationExceptionCode.ID_EXHAUSTED,
    loggingMessage = "ID가 소진되었습니다: type=$type",
)
