package org.goodee.startup_BE.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.storage.root}")
    private String fileStorageRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // OS에 상관없이(윈도우/리눅스/맥) 올바른 경로 표현을 생성해줌
        String resourceLocation = Paths.get(fileStorageRoot).toUri().toString();

        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        // /uploads/** 형태의 URL 요청이 오면
        // 위에서 설정한 file: URI 경로에서 리소스를 찾아 제공하도록 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}