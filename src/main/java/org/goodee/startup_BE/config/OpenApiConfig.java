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


        // 4. OpenAPI 객체 생성 및 반환
        return new OpenAPI()
                .info(info);
    }
}