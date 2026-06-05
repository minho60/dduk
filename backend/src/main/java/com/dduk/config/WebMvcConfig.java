package com.dduk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.frontend.path:}")
    private String configuredFrontendPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (configuredFrontendPath != null && !configuredFrontendPath.trim().isEmpty()) {
            if (configuredFrontendPath.startsWith("classpath:")) {
                registry.addResourceHandler("/**")
                        .addResourceLocations(configuredFrontendPath);
                return;
            } else {
                String normalizedPath = Paths.get(configuredFrontendPath).toAbsolutePath().toUri().toString();
                if (!normalizedPath.endsWith("/")) {
                    normalizedPath += "/";
                }
                registry.addResourceHandler("/**")
                        .addResourceLocations(normalizedPath);
                return;
            }
        }

        // 1단계: 파일 시스템에서 frontend/index.html의 존재 여부 탐색 (로컬 개발 시나리오)
        Path currentPath = Paths.get("").toAbsolutePath().normalize();
        Path resolvedPath = null;
        for (Path path = currentPath; path != null; path = path.getParent()) {
            Path tempPath = path.resolve("frontend");
            if (Files.isRegularFile(tempPath.resolve("index.html"))) {
                resolvedPath = tempPath;
                break;
            }
        }

        // 2단계: 로컬 물리 폴더가 있으면 해당 경로 사용, 없으면 JAR 내부 classpath 사용 (배포 시나리오)
        if (resolvedPath != null) {
            String defaultLocation = resolvedPath.toUri().toString();
            if (!defaultLocation.endsWith("/")) {
                defaultLocation += "/";
            }
            registry.addResourceHandler("/**")
                    .addResourceLocations(defaultLocation);
        } else {
            registry.addResourceHandler("/**")
                    .addResourceLocations("classpath:/static/");
        }
    }

    private Path resolveFrontendPath() {
        Path currentPath = Paths.get("").toAbsolutePath().normalize();

        for (Path path = currentPath; path != null; path = path.getParent()) {
            Path frontendPath = path.resolve("frontend");
            if (Files.isRegularFile(frontendPath.resolve("index.html"))) {
                return frontendPath;
            }
        }

        return currentPath.resolve("frontend");
    }
}
