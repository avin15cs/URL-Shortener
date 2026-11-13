package com.urlshortener.controller;

import com.urlshortener.dto.ApiResponse;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.service.UrlService;
import com.urlshortener.security.CurrentActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final CurrentActorResolver actorResolver;

    /**
     * POST /api/urls/shorten
     * Shorten a new URL.
     */
    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<UrlResponse>> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request,
            Authentication authentication) {
        UrlResponse response = urlService.shortenUrl(request, actorResolver.resolve(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "URL shortened successfully"));
    }

    /**
     * GET /api/urls
     * Retrieve all active, non-expired shortened URLs.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UrlResponse>>> getAllUrls(Authentication authentication) {
        List<UrlResponse> urls = urlService.getAllUrls(actorResolver.resolve(authentication));
        return ResponseEntity.ok(ApiResponse.success(urls, "URLs retrieved successfully"));
    }

    /**
     * GET /api/urls/{id}
     * Get details of a specific shortened URL.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlById(
            @PathVariable Long id, Authentication authentication) {
        UrlResponse response = urlService.getUrlById(id, actorResolver.resolve(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "URL retrieved successfully"));
    }

    /**
     * DELETE /api/urls/{id}
     * Soft-delete a shortened URL.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @PathVariable Long id, Authentication authentication) {
        urlService.deleteUrl(id, actorResolver.resolve(authentication));
        return ResponseEntity.ok(ApiResponse.success(null, "URL deleted successfully"));
    }

    /**
     * GET /api/urls/{id}/stats
     * Get click statistics for a shortened URL.
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlStats(
            @PathVariable Long id, Authentication authentication) {
        UrlResponse response = urlService.getUrlStats(id, actorResolver.resolve(authentication));
        return ResponseEntity.ok(ApiResponse.success(response, "Stats retrieved successfully"));
    }
}
