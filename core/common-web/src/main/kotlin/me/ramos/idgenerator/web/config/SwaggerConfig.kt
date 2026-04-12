package me.ramos.idgenerator.web.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * SpringDoc OpenAPI(Swagger) 공통 설정.
 *
 * @author HakHyeon Song
 */
@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("ID Generator API")
                    .description("Kotlin 기반 Random ID Generator + 분산락 테스트 프로젝트")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("HakHyeon Song")
                    ),
            )
}
