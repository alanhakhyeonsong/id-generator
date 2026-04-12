package me.ramos.idgenerator.application.exception

class IdExhaustedException(
    type: String,
) : RuntimeException("ID가 소진되었습니다: type=$type")
