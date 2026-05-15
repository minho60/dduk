package com.dduk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로젝트 루트의 frontend 폴더를 직접 매핑 (개발 모드)
        // backend/src/main/java/... 위치 기준으로 상위로 올라가서 frontend 폴더를 찾음
        String frontendPath = Paths.get("../frontend").toAbsolutePath().toUri().toString();
        
        registry.addResourceHandler("/**")
                .addResourceLocations(frontendPath + "/")
                .addResourceLocations("classpath:/static/");
    }
}
