package com.urlshortener.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ActorContext(
        SubjectType subjectType,
        UUID userId,
        String clientId,
        String name,
        String email,
        String picture,
        Set<String> roles,
        Set<String> scopes) {

    public static final String URL_ADMIN = "URL_ADMIN";

    public ActorContext {
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(roles));
        scopes = scopes == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
    }

    public static ActorContext from(Jwt jwt) {
        String subjectType = jwt.getClaimAsString("subject_type");
        Set<String> roles = stringSet(jwt.getClaim("roles"));
        Set<String> scopes = scopes(jwt.getClaim("scope"));

        if ("user".equals(subjectType)) {
            String uid = jwt.getClaimAsString("uid");
            if (uid == null || uid.isBlank()) {
                throw new AccessDeniedException("User token is missing the uid claim");
            }
            try {
                return new ActorContext(
                        SubjectType.USER,
                        UUID.fromString(uid),
                        null,
                        jwt.getClaimAsString("name"),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("picture"),
                        roles,
                        scopes);
            } catch (IllegalArgumentException exception) {
                throw new AccessDeniedException("User token contains an invalid uid claim", exception);
            }
        }

        if ("client".equals(subjectType)) {
            String clientId = jwt.getClaimAsString("client_id");
            if (clientId == null || clientId.isBlank()) {
                throw new AccessDeniedException("Client token is missing the client_id claim");
            }
            if (!roles.isEmpty()) {
                throw new AccessDeniedException("Client tokens cannot contain human roles");
            }
            return new ActorContext(
                    SubjectType.CLIENT, null, clientId, clientId, null, null, Set.of(), scopes);
        }

        throw new AccessDeniedException("Token has an unsupported subject_type");
    }

    public boolean isAdmin() {
        return subjectType == SubjectType.USER && roles.contains(URL_ADMIN);
    }

    public void requireScope(String scope) {
        if (!scopes.contains(scope)) {
            throw new AccessDeniedException("Required scope is missing: " + scope);
        }
    }

    private static Set<String> scopes(Object claim) {
        if (claim instanceof String value) {
            return Set.of(value.trim().split("\\s+"));
        }
        return stringSet(claim);
    }

    private static Set<String> stringSet(Object claim) {
        if (!(claim instanceof List<?> values)) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(value -> !value.isBlank())
                .forEach(result::add);
        return result;
    }

    public enum SubjectType {
        USER,
        CLIENT
    }
}
