package me.ramos.idgenerator.application.service

import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.application.port.`in`.InvalidateCacheInPort
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 캐시 무효화 유스케이스.
 *
 * 특정 타입 또는 전체 캐시를 삭제한다.
 *
 * @author HakHyeon Song
 */
@Service
class InvalidateCacheUseCase(
    private val cacheOutPort: CacheIdGeneratorOutPort,
) : InvalidateCacheInPort {

    override fun execute(type: String?) {
        if (type != null) {
            cacheOutPort.evict(type)
            log.info { "캐시 무효화 완료: type=$type" }
        } else {
            cacheOutPort.evictAll()
            log.info { "전체 캐시 무효화 완료" }
        }
    }
}
