package me.ramos.idgenerator.web.config

import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Configuration

/**
 * FeignClient 스캔 및 기본 설정 바인딩.
 *
 * @author HakHyeon Song
 */
@Configuration
@EnableFeignClients(
    basePackages = ["me.ramos.idgenerator"],
    defaultConfiguration = [BaseFeignConfig::class],
)
class FeignClientsScanConfig
