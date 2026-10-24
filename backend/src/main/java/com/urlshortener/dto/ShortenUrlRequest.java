package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShortenUrlRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
        regexp = "^(https?://).+",
        message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    @Size(max = 50, message = "Custom alias must not exceed 50 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]*$",
        message = "Custom alias can only contain letters, numbers, hyphens, and underscores"
    )
    private String customAlias;

    private String title;

    private String expiresAt; // ISO date string, nullable
}
