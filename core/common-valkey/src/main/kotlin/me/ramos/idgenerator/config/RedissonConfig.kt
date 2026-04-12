package me.ramos.idgenerator.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.misc.RedisURI
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redisson 클라이언트 설정.
 *
 * 모든 프로파일에서 Sentinel 구성을 사용한다.
 * Spring Boot의 [RedisProperties]를 통해 설정을 주입받는다.
 *
 * 로컬 Docker 환경에서는 Sentinel이 master를 Docker 내부 호스트명으로 보고하므로,
 * [NatMapper][org.redisson.connection.NatMapper]를 사용하여 `127.0.0.1`로 변환한다.
 *
 * @author HakHyeon Song
 */
@Configuration
class RedissonConfig {

    @Bean
    fun redissonClient(redisProperties: RedisProperties): RedissonClient {
        val config = Config()
        val sentinel = redisProperties.sentinel

        requireNotNull(sentinel) { "spring.data.redis.sentinel 설정이 필요합니다." }

        val sentinelConfig = config.useSentinelServers()
            .setMasterName(sentinel.master)
            .setCheckSentinelsList(false)
            .setNatMapper { address ->
                // Docker 내부 호스트명 → 로컬 호스트로 변환
                RedisURI(address.scheme, "127.0.0.1", address.port)
            }

        sentinel.nodes.forEach { node ->
            sentinelConfig.addSentinelAddress("redis://$node")
        }

        val password = redisProperties.password
        if (!password.isNullOrBlank()) {
            sentinelConfig.setPassword(password)
        }

        return Redisson.create(config)
    }
}
