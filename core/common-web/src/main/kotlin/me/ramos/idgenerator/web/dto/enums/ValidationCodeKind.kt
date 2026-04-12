package me.ramos.idgenerator.web.dto.enums

/**
 * Validation 에러 세부 코드.
 *
 * Jakarta Validation 어노테이션별로 고유한 결과 코드를 매핑한다.
 *
 * @author HakHyeon Song
 */
enum class ValidationCodeKind(
    val resultCode: Int,
) {
    NotNull(42001),
    NotBlank(42002),
    Size(42003),
    Min(42004),
    Max(42005),
    Pattern(42006),
    Email(42007),
    GENERIC(42000),
    ;

    companion object {
        fun fromConstraint(name: String?): ValidationCodeKind =
            entries.firstOrNull { it.name.equals(name, true) } ?: GENERIC
    }
}
