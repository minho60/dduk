package com.dduk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendPath = Paths.get("../frontend").toAbsolutePath().normalize();
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + frontendPath.toString() + "/");
    }
}
