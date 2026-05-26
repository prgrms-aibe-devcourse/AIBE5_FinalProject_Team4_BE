package com.closetnangam.be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI(@Value("${server.port:8080}") int serverPort) {
        return new OpenAPI()
                .info(new Info()
                        .title("옷장난감 API")
                        .description("AI 기반 패션 추천 서비스 API 문서")
                        .version("v1.0.0"))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("로컬 서버"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
