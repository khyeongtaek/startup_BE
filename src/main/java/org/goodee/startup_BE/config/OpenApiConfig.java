package org.goodee.startup_BE.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        // 1. API 기본 정보 설정
        Info info = new Info()
                .title("Startup_BE API Document")
                .version("v1.0.0")
                .description("스타트업 BE 프로젝트의 API 명세서입니다.");

        // 2. 보안 스킴(SecurityScheme) 정의
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // 보안 스킴 타입: HTTP
                .scheme("bearer")               // 스킴: bearer (JWT를 의미)
                .bearerFormat("JWT")            // Bearer 포맷: JWT
                .description("JWT Access Token");

        // 3. OpenAPI 컴포넌트에 보안 스킴 추가 - 전역 "Authorize" 버튼 생성
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", bearerAuth);


        // 4. OpenAPI 객체 생성 및 반환
        return new OpenAPI()
                .info(info)
                .components(components);
    }
}