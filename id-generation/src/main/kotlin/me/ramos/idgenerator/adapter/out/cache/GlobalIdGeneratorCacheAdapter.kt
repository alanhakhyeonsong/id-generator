package me.ramos.idgenerator.adapter.out.cache

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import me.ramos.idgenerator.adapter.out.persistence.entity.UsedIdJpaEntity
import me.ramos.idgenerator.application.port.out.CacheIdGeneratorOutPort
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}
private const val KEY_PREFIX = "GLOBAL:ID-GENERATOR:"

@Component
class GlobalIdGeneratorCacheAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : CacheIdGeneratorOutPort {

    override fun put(type: String, entity: UsedIdJpaEntity): UsedIdJpaEntity {
        val key = KEY_PREFIX + type
        val json = objectMapper.writeValueAsString(entity)
        redisTemplate.opsForValue().set(key, json)
        log.debug { "스토리지 업데이트: key=$key" }
        return entity
    }

    override fun getOrLoad(type: String, loader: () -> UsedIdJpaEntity): UsedIdJpaEntity {
        val key = KEY_PREFIX + type
        val cached = redisTemplate.opsForValue().get(key)

        if (cached != null) {
            log.debug { "캐시 히트: key=$key" }
            return objectMapper.readValue(cached, UsedIdJpaEntity::class.java)
        }

        log.debug { "캐시 미스, DB에서 로드: key=$key" }
        val entity = loader()
        val json = objectMapper.writeValueAsString(entity)
        redisTemplate.opsForValue().set(key, json)
        return entity
    }
}
