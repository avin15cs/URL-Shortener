package com.urlshortener.application.query;

import com.urlshortener.security.ActorContext;

public record GetUrlByIdQuery(Long id, ActorContext actor) {
}
