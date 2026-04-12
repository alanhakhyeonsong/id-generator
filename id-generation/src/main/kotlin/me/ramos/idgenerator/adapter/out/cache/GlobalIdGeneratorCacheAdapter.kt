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
        try {
            val json = objectMapper.writeValueAsString(entity)
            redisTemplate.opsForValue().set(key, json)
            log.debug { "스토리지 업데이트: key=$key" }
        } catch (e: Exception) {
            log.warn(e) { "캐시 저장 실패 (무시): key=$key" }
        }
        return entity
    }

    override fun evict(type: String) {
        val key = KEY_PREFIX + type
        try {
            redisTemplate.delete(key)
            log.info { "캐시 삭제: key=$key" }
        } catch (e: Exception) {
            log.warn(e) { "캐시 삭제 실패 (무시): key=$key" }
        }
    }

    override fun evictAll() {
        try {
            val keys = redisTemplate.keys("$KEY_PREFIX*")
            if (keys.isNotEmpty()) {
                redisTemplate.delete(keys)
                log.info { "캐시 전체 삭제: count=${keys.size}" }
            }
        } catch (e: Exception) {
            log.warn(e) { "캐시 전체 삭제 실패 (무시)" }
        }
    }

    override fun getOrLoad(type: String, loader: () -> UsedIdJpaEntity): UsedIdJpaEntity {
        val key = KEY_PREFIX + type
        val cached = try {
            redisTemplate.opsForValue().get(key)
        } catch (e: Exception) {
            log.warn(e) { "캐시 조회 실패, DB fallback: key=$key" }
            null
        }

        if (cached != null) {
            log.debug { "캐시 히트: key=$key" }
            return objectMapper.readValue(cached, UsedIdJpaEntity::class.java)
        }

        log.debug { "캐시 미스, DB에서 로드: key=$key" }
        val entity = loader()
        try {
            val json = objectMapper.writeValueAsString(entity)
            redisTemplate.opsForValue().set(key, json)
        } catch (e: Exception) {
            log.warn(e) { "캐시 저장 실패 (무시): key=$key" }
        }
        return entity
    }
}
