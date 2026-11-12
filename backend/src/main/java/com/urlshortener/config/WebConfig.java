package com.urlshortener.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = appProperties.getCors().getAllowedOrigins();
        String[] allowedOrigins = (origins != null && !origins.isEmpty())
                ? origins.toArray(new String[0])
                : new String[]{"http://localhost:5173", "http://localhost:3000"};

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Automatically redirect requests without trailing slash or legacy /h2-console to /api/h2-console/
        registry.addRedirectViewController("/api/h2-console", "/api/h2-console/");
        registry.addRedirectViewController("/h2-console", "/api/h2-console/");
        registry.addRedirectViewController("/h2-console/", "/api/h2-console/");
    }
}
