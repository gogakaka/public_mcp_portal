package com.umg.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI 문서화 설정.
 *
 * <p>Swagger UI ({@code /swagger-ui.html})와 OpenAPI spec ({@code /v3/api-docs})을
 * 제공하며, JWT Bearer 토큰과 API Key 인증 스킴을 정의합니다.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Universal MCP Gateway API")
                        .description("MCP 도구 관리, 실행 및 모니터링을 위한 통합 게이트웨이 API.\n\n"
                                + "**인증 방식:**\n"
                                + "- JWT Bearer Token: 웹 UI 사용자용\n"
                                + "- API Key (X-API-Key 헤더): 에이전트/자동화용")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UMG Team"))
                        .license(new License()
                                .name("Private")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("apiKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 액세스 토큰"))
                        .addSecuritySchemes("apiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API 키 인증")));
    }
}
