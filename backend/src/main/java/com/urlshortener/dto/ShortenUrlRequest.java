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
        regexp = "^$|^[a-zA-Z0-9_-]{3,50}$",
        message = "Custom alias must be 3 to 50 letters, numbers, hyphens, or underscores"
    )
    private String customAlias;

    private String title;

    private String expiresAt; // ISO date string, nullable
}
