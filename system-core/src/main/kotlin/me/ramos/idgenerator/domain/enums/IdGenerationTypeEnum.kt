package me.ramos.idgenerator.domain.enums

/**
 * ID 생성 타입별 접두사를 정의하는 열거형.
 *
 * @property prefix 생성되는 ID의 접두사 (2자리)
 * @author HakHyeon Song
 */
enum class IdGenerationTypeEnum(
    val prefix: String,
) {
    BACKUP("MB"),
    RECOVERY("MR"),
    AGENT("AG"),
    BACKUP_REPORT("BR"),
    RECOVERY_REPORT("RR"),
    BARE_METAL("BM"),
    POLICY_NAME("PM"),
    ;

    companion object {
        fun fromPrefix(prefix: String): IdGenerationTypeEnum =
            entries.first { it.prefix == prefix }
    }
}
