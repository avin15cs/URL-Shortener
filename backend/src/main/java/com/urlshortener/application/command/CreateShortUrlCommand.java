package com.urlshortener.application.command;

import com.urlshortener.security.ActorContext;

public record CreateShortUrlCommand(
        String originalUrl,
        String customAlias,
        String title,
        String expiresAt,
        ActorContext actor) {
}
