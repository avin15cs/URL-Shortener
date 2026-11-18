package com.urlshortener.security;

import com.urlshortener.model.Url;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UrlAccessPolicy {

    public void requireOwnerOrAdmin(Url url, ActorContext actor) {
        if (actor.isAdmin()) {
            return;
        }

        boolean owner = actor.subjectType() == ActorContext.SubjectType.USER
                ? Objects.equals(actor.userId(), url.getOwnerUid())
                : Objects.equals(actor.clientId(), url.getOwnerClientId());
        if (!owner) {
            throw new AccessDeniedException("This URL belongs to another subject");
        }
    }
}
