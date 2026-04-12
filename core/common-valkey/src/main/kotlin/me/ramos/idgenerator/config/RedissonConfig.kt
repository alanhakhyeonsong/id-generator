package me.ramos.idgenerator.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Redisson 클라이언트 설정.
 *
 * 모든 프로파일에서 Sentinel 구성을 사용한다.
 * Sentinel 노드 목록과 master 이름은 프로파일별 application-valkey-*.yaml에서 주입된다.
 *
 * @author HakHyeon Song
 */
@Configuration
class RedissonConfig {

    @Bean
    fun redissonClient(
        @Value("\${spring.data.redis.sentinel.master}") masterName: String,
        @Value("\${spring.data.redis.sentinel.nodes}") sentinelNodes: String,
        @Value("\${spring.data.redis.password:}") password: String,
    ): RedissonClient {
        val config = Config()
        val sentinelConfig = config.useSentinelServers()
            .setMasterName(masterName)
            .setCheckSentinelsList(false)

        sentinelNodes.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { node ->
                sentinelConfig.addSentinelAddress("redis://$node")
            }

        if (password.isNotBlank()) {
            sentinelConfig.setPassword(password)
        }

        return Redisson.create(config)
    }
}
