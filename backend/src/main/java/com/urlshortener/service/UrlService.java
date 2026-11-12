package com.urlshortener.service;

import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlAlreadyExistsException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sqids.Sqids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;
    private final AppProperties appProperties;
    private final Sqids sqids;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final String PENDING_CODE_PREFIX = "~pending_";

    /**
     * Shorten a URL and persist it.
     */
    @Transactional
    public UrlResponse shortenUrl(ShortenUrlRequest request) {
        String customAlias = normalizeAlias(request.getCustomAlias());

        // Custom aliases and generated codes share one public namespace.
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
        if (request.getExpiresAt() != null && !request.getExpiresAt().isBlank()) {
            expiresAt = LocalDateTime.parse(request.getExpiresAt(), DateTimeFormatter.ISO_DATE_TIME);
        }

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(customAlias != null ? customAlias : pendingCode())
                .customAlias(customAlias)
                .title(request.getTitle())
                .expiresAt(expiresAt)
                .build();

        // IDENTITY IDs are assigned by the first flush. The temporary code is never
        // visible outside this transaction and is replaced with the Sqid immediately.
        Url saved = urlRepository.saveAndFlush(url);
        if (customAlias == null) {
            saved.setShortCode(generateUniqueShortCode(saved.getId()));
            saved = urlRepository.saveAndFlush(saved);
        }

        log.info("Created short URL: {} -> {}", saved.getShortCode(), saved.getOriginalUrl());
        return mapToResponse(saved);
    }

    /**
     * Get all active, non-expired URLs.
     */
    @Transactional(readOnly = true)
    public List<UrlResponse> getAllUrls() {
        return urlRepository.findAllActiveAndNonExpired()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single URL by its database ID.
     */
    @Transactional(readOnly = true)
    public UrlResponse getUrlById(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL with ID " + id + " not found"));
        return mapToResponse(url);
    }

    /**
     * Resolve a short code (or alias) to an original URL and increment click count.
     */
    @Transactional
    public String resolveShortCode(String code) {
        Url url = urlRepository.findByShortCode(code)
                // Compatibility fallback for rows created before aliases became the canonical short code.
                .or(() -> urlRepository.findByCustomAlias(code))
                .orElseThrow(() -> new UrlNotFoundException("Short URL '" + code + "' not found"));

        if (!url.getIsActive()) {
            throw new UrlNotFoundException("Short URL '" + code + "' is no longer active");
        }
        if (url.isExpired()) {
            throw new UrlNotFoundException("Short URL '" + code + "' has expired");
        }

        urlRepository.incrementClickCount(url.getId());
        log.info("Redirecting {} -> {}", code, url.getOriginalUrl());
        return url.getOriginalUrl();
    }

    /**
     * Delete (soft delete) a URL by its ID.
     */
    @Transactional
    public void deleteUrl(Long id) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new UrlNotFoundException("URL with ID " + id + " not found"));
        url.setIsActive(false);
        urlRepository.save(url);
        log.info("Deactivated URL with ID: {}", id);
    }

    /**
     * Get click statistics for a URL.
     */
    @Transactional(readOnly = true)
    public UrlResponse getUrlStats(Long id) {
        return getUrlById(id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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
                "Unable to generate a unique short code after " + MAX_CODE_GENERATION_ATTEMPTS + " attempts");
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

    private UrlResponse mapToResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .customAlias(url.getCustomAlias())
                .title(url.getTitle())
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .updatedAt(url.getUpdatedAt())
                .expiresAt(url.getExpiresAt())
                .isActive(url.getIsActive())
                .build();
    }
}
