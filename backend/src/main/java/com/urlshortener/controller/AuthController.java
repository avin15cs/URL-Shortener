package com.urlshortener.controller;

import com.urlshortener.security.ActorContext;
import com.urlshortener.security.CurrentActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentActorResolver actorResolver;

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication, CsrfToken csrfToken) {
        return actorResolver.tryResolve(authentication)
                .map(actor -> authenticated(actor, csrfToken))
                .orElseGet(() -> unauthenticated(csrfToken));
    }

    private SessionResponse authenticated(ActorContext actor, CsrfToken csrfToken) {
        return new SessionResponse(
                true,
                actor.subjectType().name().toLowerCase(),
                actor.name(),
                actor.email(),
                actor.picture(),
                actor.roles(),
                actor.isAdmin(),
                csrfToken.getHeaderName(),
                csrfToken.getToken());
    }

    private SessionResponse unauthenticated(CsrfToken csrfToken) {
        return new SessionResponse(
                false, null, null, null, null, Set.of(), false,
                csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public record SessionResponse(
            boolean authenticated,
            String subjectType,
            String name,
            String email,
            String picture,
            Set<String> roles,
            boolean admin,
            String csrfHeader,
            String csrfToken) {
    }
}
