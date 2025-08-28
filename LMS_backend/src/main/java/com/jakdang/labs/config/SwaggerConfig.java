package com.jakdang.labs.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Value("${springdoc.swagger-ui.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Kafka 게시판 API")
                .version("v1.0")
                .description("Kafka 기반 게시판 서비스 API 문서")
                .license(new License().name("Apache 2.0").url("http://springdoc.org"));

        // API 서버 정보 설정
        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription("API 서버");

        // 보안 스키마 설정 (JWT 토큰 인증 사용 시)
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .security(Arrays.asList(securityRequirement))
                .info(info)
                .servers(Arrays.asList(server));
    }
}