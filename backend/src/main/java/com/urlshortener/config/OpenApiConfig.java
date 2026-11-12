package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("""
                                Production-grade URL Shortener REST API built with Spring Boot 4 & Java 21.
                                
                                **Features:**
                                - Shorten any URL to a Sqids code with a minimum length of 7 characters
                                - Custom alias support
                                - Click analytics & statistics
                                - URL expiry support
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener")
                                .url("https://github.com/avin15cs/URL-Shortener"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Local Development Server"),
                        new Server().url("https://yourdomain.com").description("Production Server")
                ));
    }
}
