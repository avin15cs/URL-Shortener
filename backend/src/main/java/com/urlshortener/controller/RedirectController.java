package com.urlshortener.controller;

import com.urlshortener.config.AppProperties;
import com.urlshortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;
    private final AppProperties appProperties;

    /**
     * GET /{shortCode}
     * Redirect to the original URL using a 302 Found response.
     * Guards against reserved paths configured in application.yml.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        if ("h2-console".equalsIgnoreCase(shortCode)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/api/h2-console/"));
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
        }

        if (appProperties.getReservedPaths() != null &&
                appProperties.getReservedPaths().contains(shortCode.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "'" + shortCode + "' is a reserved path");
        }

        String originalUrl = urlService.resolveShortCode(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
