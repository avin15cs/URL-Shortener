package com.urlshortener.application.command;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlAlreadyExistsException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.security.ActorContext;
import com.urlshortener.security.UrlAccessPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sqids.Sqids;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCommandService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final String PENDING_CODE_PREFIX = "~pending_";

    private final UrlRepository urlRepository;
    private final AppProperties appProperties;
    private final Sqids sqids;
    private final UrlResponseMapper responseMapper;
    private final UrlAccessPolicy accessPolicy;

    @Transactional
    public UrlResponse handle(CreateShortUrlCommand command) {
        command.actor().requireScope("urls.write");
        String customAlias = normalizeAlias(command.customAlias());

        if (customAlias != null) {
            if (isReserved(customAlias)) {
                throw new IllegalArgumentException(
                        "Custom alias '" + customAlias + "' is a reserved keyword");
            }
            if (urlRepository.existsByShortCode(customAlias)
                    || urlRepository.existsByCustomAlias(customAlias)) {
                throw new UrlAlreadyExistsException(
                        "Custom alias '" + customAlias + "' is already taken");
            }
        }

        LocalDateTime expiresAt = null;
        if (command.expiresAt() != null && !command.expiresAt().isBlank()) {
            expiresAt = LocalDateTime.parse(command.expiresAt(), DateTimeFormatter.ISO_DATE_TIME);
        }

        Url url = Url.builder()
                .originalUrl(command.originalUrl())
                .shortCode(customAlias != null ? customAlias : pendingCode())
                .customAlias(customAlias)
                .title(command.title())
                .expiresAt(expiresAt)
                .ownerUid(command.actor().subjectType() == ActorContext.SubjectType.USER
                        ? command.actor().userId() : null)
                .ownerClientId(command.actor().subjectType() == ActorContext.SubjectType.CLIENT
                        ? command.actor().clientId() : null)
                .build();

        Url saved = urlRepository.saveAndFlush(url);
        if (customAlias == null) {
            saved.setShortCode(generateUniqueShortCode(saved.getId()));
            saved = urlRepository.saveAndFlush(saved);
        }

        log.info("Created short URL: {} -> {}", saved.getShortCode(), saved.getOriginalUrl());
        return responseMapper.toResponse(saved);
    }

    @Transactional
    public void handle(DeactivateUrlCommand command) {
        command.actor().requireScope("urls.write");
        Url url = urlRepository.findById(command.id())
                .orElseThrow(() -> new UrlNotFoundException(
                        "URL with ID " + command.id() + " not found"));
        accessPolicy.requireOwnerOrAdmin(url, command.actor());
        url.setIsActive(false);
        urlRepository.save(url);
        log.info("Deactivated URL with ID: {}", command.id());
    }

    @Transactional
    public void handle(RecordClickCommand command) {
        urlRepository.incrementClickCount(command.urlId());
    }

    private String generateUniqueShortCode(Long id) {
        if (id == null) {
            throw new IllegalStateException("Cannot generate a short code before the URL has an ID");
        }

        for (long discriminator = 0; discriminator < MAX_CODE_GENERATION_ATTEMPTS; discriminator++) {
            List<Long> input = new ArrayList<>();
            input.add(id);
            if (discriminator > 0) {
                input.add(discriminator);
            }

            String code = sqids.encode(input);
            if (!isReserved(code)
                    && !urlRepository.existsByShortCode(code)
                    && !urlRepository.existsByCustomAlias(code)) {
                return code;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code after "
                        + MAX_CODE_GENERATION_ATTEMPTS + " attempts");
    }

    private String normalizeAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return null;
        }
        return customAlias.trim();
    }

    private boolean isReserved(String code) {
        return appProperties.getReservedPaths() != null
                && appProperties.getReservedPaths().contains(code.toLowerCase(Locale.ROOT));
    }

    private String pendingCode() {
        return PENDING_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }
}
