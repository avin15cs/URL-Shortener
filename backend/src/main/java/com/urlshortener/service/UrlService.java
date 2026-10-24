package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlAlreadyExistsException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private final Random random = new Random();

    /**
     * Shorten a URL and persist it.
     */
    @Transactional
    public UrlResponse shortenUrl(ShortenUrlRequest request) {
        // Validate custom alias uniqueness
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (urlRepository.existsByCustomAlias(request.getCustomAlias())) {
                throw new UrlAlreadyExistsException(
                        "Custom alias '" + request.getCustomAlias() + "' is already taken");
            }
        }

        String shortCode = generateUniqueShortCode();

        LocalDateTime expiresAt = null;
        if (request.getExpiresAt() != null && !request.getExpiresAt().isBlank()) {
            expiresAt = LocalDateTime.parse(request.getExpiresAt(), DateTimeFormatter.ISO_DATE_TIME);
        }

        Url url = Url.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .customAlias(request.getCustomAlias())
                .title(request.getTitle())
                .expiresAt(expiresAt)
                .build();

        Url saved = urlRepository.save(url);
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

    private String generateUniqueShortCode() {
        String code;
        int attempts = 0;
        do {
            code = generateShortCode();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate a unique short code after 10 attempts");
            }
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private UrlResponse mapToResponse(Url url) {
        String effectiveCode = url.getCustomAlias() != null ? url.getCustomAlias() : url.getShortCode();
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + effectiveCode)
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
