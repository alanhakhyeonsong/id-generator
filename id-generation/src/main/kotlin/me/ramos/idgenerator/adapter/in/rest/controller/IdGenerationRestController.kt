package me.ramos.idgenerator.adapter.`in`.rest.controller

import me.ramos.idgenerator.application.port.`in`.AddIdTypeInPort
import me.ramos.idgenerator.application.port.`in`.BatchInsertIdInPort
import me.ramos.idgenerator.application.port.`in`.GenerateIdInPort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ID 생성 REST API 컨트롤러.
 *
 * @author HakHyeon Song
 */
@RestController
@RequestMapping("/api/v1/id-generation")
class IdGenerationRestController(
    private val generateIdInPort: GenerateIdInPort,
    private val batchInsertIdInPort: BatchInsertIdInPort,
    private val addIdTypeInPort: AddIdTypeInPort,
) {

    @PostMapping("/batch")
    fun batchInsertId(): ResponseEntity<String> {
        batchInsertIdInPort.execute()
        return ResponseEntity.ok("배치 ID 삽입 완료")
    }

    @PostMapping("/types/{type}")
    fun addType(@PathVariable type: String): ResponseEntity<String> {
        addIdTypeInPort.execute(type)
        return ResponseEntity.ok("타입 추가 완료: $type")
    }

    @PostMapping("/{type}")
    fun generateId(@PathVariable type: String): ResponseEntity<String> {
        val id = generateIdInPort.execute(type)
        return ResponseEntity.ok(id)
    }
}
