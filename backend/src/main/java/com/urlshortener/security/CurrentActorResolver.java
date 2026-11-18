package com.urlshortener.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentActorResolver {

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final JwtDecoder jwtDecoder;

    public ActorContext resolve(Authentication authentication) {
        return tryResolve(authentication)
                .orElseThrow(() -> new AccessDeniedException("An authenticated OAuth subject is required"));
    }

    public Optional<ActorContext> tryResolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(ActorContext.from(jwtAuthentication.getToken()));
        }
        if (authentication instanceof OAuth2AuthenticationToken oauthAuthentication) {
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(
                    OAuth2AuthorizeRequest
                            .withClientRegistrationId(oauthAuthentication.getAuthorizedClientRegistrationId())
                            .principal(oauthAuthentication)
                            .build());
            if (client == null || client.getAccessToken() == null) {
                throw new AccessDeniedException("The authenticated session has no access token");
            }
            Jwt jwt = jwtDecoder.decode(client.getAccessToken().getTokenValue());
            return Optional.of(ActorContext.from(jwt));
        }
        return Optional.empty();
    }
}
