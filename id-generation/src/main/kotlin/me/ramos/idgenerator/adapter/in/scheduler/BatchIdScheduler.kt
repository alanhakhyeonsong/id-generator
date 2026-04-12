package me.ramos.idgenerator.adapter.`in`.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.application.port.`in`.BatchInsertIdInPort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 배치 ID 자동 생성 스케줄러.
 *
 * 주기적으로 ID 소진 임계치(90%)를 확인하여,
 * 조건 충족 시 100,000건의 Base33 랜덤 ID를 자동 적재한다.
 *
 * @author HakHyeon Song
 */
@Component
class BatchIdScheduler(
    private val batchInsertIdInPort: BatchInsertIdInPort,
) {

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    fun checkAndGenerateBatchId() {
        try {
            batchInsertIdInPort.execute()
        } catch (e: Exception) {
            log.error(e) { "배치 ID 자동 생성 중 오류 발생" }
        }
    }
}
