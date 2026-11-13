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
import com.urlshortener.security.ActorContext;
import com.urlshortener.security.CurrentActorResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlCommandService commandService;
    private final UrlQueryService queryService;
    private final CurrentActorResolver actorResolver;

    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<UrlResponse>> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest request,
            Authentication authentication) {
        ActorContext actor = actorResolver.resolve(authentication);
        UrlResponse response = commandService.handle(new CreateShortUrlCommand(
                request.getOriginalUrl(),
                request.getCustomAlias(),
                request.getTitle(),
                request.getExpiresAt(),
                actor));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "URL shortened successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UrlResponse>>> getAllUrls(
            Authentication authentication) {
        ActorContext actor = actorResolver.resolve(authentication);
        List<UrlResponse> urls = queryService.handle(new ListUrlsQuery(actor));
        return ResponseEntity.ok(ApiResponse.success(urls, "URLs retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlById(
            @PathVariable Long id,
            Authentication authentication) {
        ActorContext actor = actorResolver.resolve(authentication);
        UrlResponse response = queryService.handle(new GetUrlByIdQuery(id, actor));
        return ResponseEntity.ok(ApiResponse.success(response, "URL retrieved successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(
            @PathVariable Long id,
            Authentication authentication) {
        ActorContext actor = actorResolver.resolve(authentication);
        commandService.handle(new DeactivateUrlCommand(id, actor));
        return ResponseEntity.ok(ApiResponse.success(null, "URL deleted successfully"));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlStats(
            @PathVariable Long id,
            Authentication authentication) {
        ActorContext actor = actorResolver.resolve(authentication);
        UrlResponse response = queryService.handle(new GetUrlStatsQuery(id, actor));
        return ResponseEntity.ok(ApiResponse.success(response, "Stats retrieved successfully"));
    }
}
