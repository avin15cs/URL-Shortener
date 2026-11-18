package com.urlshortener.application.command;

public record CreateShortUrlCommand(
        String originalUrl,
        String customAlias,
        String title,
        String expiresAt) {
}
