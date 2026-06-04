package com.dduk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendPath = resolveFrontendPath();
        registry.addResourceHandler("/**")
                .addResourceLocations(frontendPath.toUri().toString());
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
