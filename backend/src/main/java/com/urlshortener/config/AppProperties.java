package com.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Base URL for generated short links (e.g. http://localhost:8080 or https://mydomain.com).
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * Single-segment paths that are reserved by system tooling (Actuator, Swagger, static assets, etc.)
     * and must never be matched or claimed as short codes / custom aliases.
     */
    private Set<String> reservedPaths = new HashSet<>(Set.of(
            "actuator",
            "swagger-ui",
            "swagger-ui.html",
            "v3",
            "webjars",
            "favicon.ico",
            "error",
            "api",
            "h2-console"
    ));

    /**
     * CORS configuration.
     */
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
    }
}
