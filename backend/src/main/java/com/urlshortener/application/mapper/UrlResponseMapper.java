package com.urlshortener.application.mapper;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.Url;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UrlResponseMapper {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlResponse toResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .customAlias(url.getCustomAlias())
                .title(url.getTitle())
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .updatedAt(url.getUpdatedAt())
                .expiresAt(url.getExpiresAt())
                .isActive(url.getIsActive())
                .build();
    }
}
