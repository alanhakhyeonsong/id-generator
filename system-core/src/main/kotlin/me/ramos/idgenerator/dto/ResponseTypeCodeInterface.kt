package me.ramos.idgenerator.dto

/**
 * 모든 에러 코드(enum)가 구현하는 공통 인터페이스.
 *
 * 모듈별로 독립적인 에러 코드 enum을 정의하되,
 * 이 인터페이스를 구현하면 [GlobalExceptionHandler][me.ramos.idgenerator.web.advice.GlobalExceptionHandler]에서
 * 일관된 에러 응답을 생성할 수 있다.
 *
 * @author HakHyeon Song
 */
interface ResponseTypeCodeInterface {
    val httpStatus: Int
    val resultCode: Int
    val message: String
}
