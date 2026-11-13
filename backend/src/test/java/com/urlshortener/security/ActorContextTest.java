package com.urlshortener.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorContextTest {

    @Test
    void from_shouldResolveUserIdentityRolesAndScopes() {
        ActorContext actor = ActorContext.from(jwt(
                "subject_type", "user",
                "uid", "11111111-1111-1111-1111-111111111111",
                "roles", List.of("URL_ADMIN"),
                "scope", "urls.read urls.write"));

        assertThat(actor.subjectType()).isEqualTo(ActorContext.SubjectType.USER);
        assertThat(actor.isAdmin()).isTrue();
        assertThat(actor.scopes()).containsExactlyInAnyOrder("urls.read", "urls.write");
    }

    @Test
    void from_shouldKeepClientIdentitySeparateFromUsers() {
        ActorContext actor = ActorContext.from(jwt(
                "subject_type", "client",
                "client_id", "url-shortener-automation",
                "scope", List.of("urls.read")));

        assertThat(actor.subjectType()).isEqualTo(ActorContext.SubjectType.CLIENT);
        assertThat(actor.clientId()).isEqualTo("url-shortener-automation");
        assertThat(actor.userId()).isNull();
        assertThat(actor.roles()).isEmpty();
    }

    @Test
    void from_shouldRejectHumanRolesOnClientTokens() {
        Jwt jwt = jwt(
                "subject_type", "client",
                "client_id", "url-shortener-automation",
                "roles", List.of("URL_ADMIN"),
                "scope", "urls.read");

        assertThatThrownBy(() -> ActorContext.from(jwt))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("cannot contain human roles");
    }

    private Jwt jwt(Object... claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        for (int index = 0; index < claims.length; index += 2) {
            builder.claim((String) claims[index], claims[index + 1]);
        }
        return builder.build();
    }
}
