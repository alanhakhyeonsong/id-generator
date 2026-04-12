package me.ramos.idgenerator.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.config.SentinelServersConfig
import org.redisson.misc.RedisURI
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/**
 * Redisson Sentinel 공통 설정.
 *
 * 모든 프로파일에서 Sentinel 구성을 사용한다.
 * Spring Boot의 [RedisProperties]를 통해 설정을 주입받는다.
 *
 * @author HakHyeon Song
 */
@Configuration
class RedissonConfig {

    /**
     * Alpha 등 비-로컬 환경용 Redisson 클라이언트.
     *
     * K8s 환경에서는 DNS로 직접 접근하므로 NatMapper가 필요 없다.
     */
    @Bean
    @Profile("!valkey-local")
    fun redissonClient(redisProperties: RedisProperties): RedissonClient {
        val config = Config()
        val sentinelConfig = configureSentinel(config, redisProperties)
        applyPassword(sentinelConfig, redisProperties)
        return Redisson.create(config)
    }

    /**
     * 로컬 Docker 환경용 Redisson 클라이언트.
     *
     * Sentinel이 master를 Docker 내부 호스트명으로 보고하므로,
     * [NatMapper][org.redisson.connection.NatMapper]를 사용하여 `127.0.0.1`로 변환한다.
     */
    @Bean
    @Profile("valkey-local")
    fun redissonLocalClient(redisProperties: RedisProperties): RedissonClient {
        val config = Config()
        val sentinelConfig = configureSentinel(config, redisProperties)
        sentinelConfig.setNatMapper { address ->
            RedisURI(address.scheme, "127.0.0.1", address.port)
        }
        applyPassword(sentinelConfig, redisProperties)
        return Redisson.create(config)
    }

    private fun configureSentinel(config: Config, redisProperties: RedisProperties): SentinelServersConfig {
        val sentinel = redisProperties.sentinel
        requireNotNull(sentinel) { "spring.data.redis.sentinel 설정이 필요합니다." }

        val sentinelConfig = config.useSentinelServers()
            .setMasterName(sentinel.master)
            .setCheckSentinelsList(false)

        sentinel.nodes.forEach { node ->
            sentinelConfig.addSentinelAddress("redis://$node")
        }

        return sentinelConfig
    }

    private fun applyPassword(sentinelConfig: SentinelServersConfig, redisProperties: RedisProperties) {
        val password = redisProperties.password
        if (!password.isNullOrBlank()) {
            sentinelConfig.setPassword(password)
        }
    }
}
