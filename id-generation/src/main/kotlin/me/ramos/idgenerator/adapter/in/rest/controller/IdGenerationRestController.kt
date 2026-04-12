package me.ramos.idgenerator.adapter.`in`.rest.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import me.ramos.idgenerator.application.port.`in`.AddIdTypeInPort
import me.ramos.idgenerator.application.port.`in`.BatchInsertIdInPort
import me.ramos.idgenerator.application.port.`in`.GenerateIdInPort
import me.ramos.idgenerator.application.port.`in`.InvalidateCacheInPort
import me.ramos.idgenerator.web.dto.CommonResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * ID 생성 REST API 컨트롤러.
 *
 * @author HakHyeon Song
 */
@RestController
@RequestMapping("/api/v1/id-generation")
@Tag(name = "ID Generation", description = "랜덤 ID 생성 API")
class IdGenerationRestController(
    private val generateIdInPort: GenerateIdInPort,
    private val batchInsertIdInPort: BatchInsertIdInPort,
    private val addIdTypeInPort: AddIdTypeInPort,
    private val invalidateCacheInPort: InvalidateCacheInPort,
) {

    @PostMapping("/batch")
    @Operation(summary = "배치 ID 삽입", description = "100,000건의 Base33 랜덤 ID 값을 사전 적재한다.")
    fun batchInsertId(): CommonResponse<String?> {
        batchInsertIdInPort.execute()
        return CommonResponse.ok("배치 ID 삽입 완료")
    }

    @PostMapping("/types/{type}")
    @Operation(summary = "ID 타입 등록", description = "새로운 ID 생성 타입을 등록한다.")
    fun addType(
        @Parameter(description = "ID 타입 접두사 (예: AG, MB, BR)", example = "AG")
        @PathVariable type: String,
    ): CommonResponse<String?> {
        addIdTypeInPort.execute(type)
        return CommonResponse.ok("타입 추가 완료: $type")
    }

    @PostMapping("/{type}")
    @Operation(summary = "ID 생성", description = "지정된 타입의 고유 ID를 생성한다. 결과 형식: {type}-{base33Value}")
    fun generateId(
        @Parameter(description = "ID 타입 접두사", example = "AG")
        @PathVariable type: String,
    ): CommonResponse<String?> {
        val id = generateIdInPort.execute(type)
        return CommonResponse.ok(id)
    }

    @DeleteMapping("/cache")
    @Operation(summary = "캐시 무효화", description = "ID 생성기 시퀀스 캐시를 삭제한다. type 미지정 시 전체 삭제.")
    fun invalidateCache(
        @Parameter(description = "ID 타입 (미지정 시 전체 삭제)", example = "BACKUP")
        @RequestParam(required = false) type: String?,
    ): CommonResponse<String?> {
        invalidateCacheInPort.execute(type)
        val target = type ?: "전체"
        return CommonResponse.ok("캐시 무효화 완료: $target")
    }
}
