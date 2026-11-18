package com.urlshortener.controller;

import com.urlshortener.application.command.CreateShortUrlCommand;
import com.urlshortener.application.command.DeactivateUrlCommand;
import com.urlshortener.application.command.UrlCommandService;
import com.urlshortener.application.query.GetUrlByIdQuery;
import com.urlshortener.application.query.GetUrlStatsQuery;
import com.urlshortener.application.query.ListUrlsQuery;
import com.urlshortener.application.query.UrlQueryService;
import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlCommandService commandService;
    private final UrlQueryService queryService;

    /**
     * POST /api/urls/shorten
     * Shorten a new URL.
     */
    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<UrlResponse>> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request) {
        UrlResponse response = commandService.handle(new CreateShortUrlCommand(
                request.getOriginalUrl(),
                request.getCustomAlias(),
                request.getTitle(),
                request.getExpiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "URL shortened successfully"));
    }

    /**
     * GET /api/urls
     * Retrieve all active, non-expired shortened URLs.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UrlResponse>>> getAllUrls() {
        List<UrlResponse> urls = queryService.handle(new ListUrlsQuery());
        return ResponseEntity.ok(ApiResponse.success(urls, "URLs retrieved successfully"));
    }

    /**
     * GET /api/urls/{id}
     * Get details of a specific shortened URL.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlById(@PathVariable Long id) {
        UrlResponse response = queryService.handle(new GetUrlByIdQuery(id));
        return ResponseEntity.ok(ApiResponse.success(response, "URL retrieved successfully"));
    }

    /**
     * DELETE /api/urls/{id}
     * Soft-delete a shortened URL.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable Long id) {
        commandService.handle(new DeactivateUrlCommand(id));
        return ResponseEntity.ok(ApiResponse.success(null, "URL deleted successfully"));
    }

    /**
     * GET /api/urls/{id}/stats
     * Get click statistics for a shortened URL.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlStats(@PathVariable Long id) {
        UrlResponse response = queryService.handle(new GetUrlStatsQuery(id));
        return ResponseEntity.ok(ApiResponse.success(response, "Stats retrieved successfully"));
    }
}
