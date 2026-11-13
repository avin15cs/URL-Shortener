package com.urlshortener.application.command;

import com.urlshortener.security.ActorContext;

public record DeactivateUrlCommand(Long id, ActorContext actor) {
}
