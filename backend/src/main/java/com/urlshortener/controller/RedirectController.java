package com.urlshortener.controller;

import com.urlshortener.application.command.RecordClickCommand;
import com.urlshortener.application.command.UrlCommandService;
import com.urlshortener.application.query.ResolveShortCodeQuery;
import com.urlshortener.application.query.ResolvedUrl;
import com.urlshortener.application.query.UrlQueryService;
import com.urlshortener.config.AppProperties;
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

    private final UrlCommandService commandService;
    private final UrlQueryService queryService;
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

        ResolvedUrl resolvedUrl = queryService.handle(new ResolveShortCodeQuery(shortCode));
        commandService.handle(new RecordClickCommand(resolvedUrl.id()));
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(resolvedUrl.originalUrl()));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }
}
