package me.ramos.idgenerator.web.config

import feign.Logger
import feign.RequestInterceptor
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean

private val log = KotlinLogging.logger {}

/**
 * OpenFeign 기본 설정.
 *
 * 주의: @Configuration이 아닌 일반 클래스. FeignClient의 configuration 속성에서 참조한다.
 * @Configuration으로 선언하면 모든 FeignClient에 전역 적용되어 의도치 않은 설정 충돌이 발생할 수 있다.
 *
 * @author HakHyeon Song
 */
class BaseFeignConfig {

    @Bean
    fun feignLoggerLevel(): Logger.Level = Logger.Level.BASIC

    @Bean
    fun feignLoggingInterceptor(): RequestInterceptor =
        RequestInterceptor { template ->
            if (!log.isInfoEnabled()) return@RequestInterceptor
            log.info { "[Feign] ${template.method()} ${template.url()}" }
        }
}
