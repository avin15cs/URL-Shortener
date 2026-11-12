package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import org.sqids.Sqids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import com.urlshortener.config.AppProperties;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private AppProperties appProperties;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setReservedPaths(Set.of("actuator", "api", "v3"));
        urlService = new UrlService(
                urlRepository,
                appProperties,
                Sqids.builder().minLength(7).build());
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void shortenUrl_shouldCreateNewShortUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com/very/long/path");

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.existsByCustomAlias(anyString())).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
            Url savedUrl = invocation.getArgument(0);
            if (savedUrl.getId() == null) {
                savedUrl.setId(1L);
            }
            return savedUrl;
        });

        UrlResponse response = urlService.shortenUrl(request);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalUrl()).isEqualTo("https://www.example.com/very/long/path");
        assertThat(response.getShortCode()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(response.getShortUrl()).endsWith("/" + response.getShortCode());
        assertThat(Sqids.builder().minLength(7).build().decode(response.getShortCode()))
                .containsExactly(1L);
        verify(urlRepository, times(2)).saveAndFlush(any(Url.class));
    }

    @Test
    void resolveShortCode_shouldReturnOriginalUrl() {
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .clickCount(0L)
                .isActive(true)
                .build();

        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(url));
        doNothing().when(urlRepository).incrementClickCount(1L);

        String result = urlService.resolveShortCode("abc1234");

        assertThat(result).isEqualTo("https://www.example.com");
        verify(urlRepository).incrementClickCount(1L);
    }

    @Test
    void resolveShortCode_shouldThrowWhenNotFound() {
        when(urlRepository.findByShortCode("notexist")).thenReturn(Optional.empty());
        when(urlRepository.findByCustomAlias("notexist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveShortCode("notexist"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void deleteUrl_shouldDeactivateUrl() {
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .isActive(true)
                .clickCount(0L)
                .build();

        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        urlService.deleteUrl(1L);

        verify(urlRepository).save(argThat(u -> !u.getIsActive()));
    }

    @Test
    void shortenUrl_shouldThrowWhenAliasIsReserved() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("actuator");

        assertThatThrownBy(() -> urlService.shortenUrl(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved keyword");
    }

    @Test
    void shortenUrl_shouldStoreCustomAliasAsCanonicalShortCode() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("my-link");

        when(urlRepository.existsByShortCode("my-link")).thenReturn(false);
        when(urlRepository.existsByCustomAlias("my-link")).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponse response = urlService.shortenUrl(request);

        assertThat(response.getShortCode()).isEqualTo("my-link");
        assertThat(response.getCustomAlias()).isEqualTo("my-link");
        assertThat(response.getShortUrl()).endsWith("/my-link");
        verify(urlRepository).saveAndFlush(argThat(url -> "my-link".equals(url.getShortCode())));
    }

    @Test
    void shortenUrl_shouldTreatBlankAliasAsAbsent() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("");

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.existsByCustomAlias(anyString())).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
            Url savedUrl = invocation.getArgument(0);
            savedUrl.setId(2L);
            return savedUrl;
        });

        UrlResponse response = urlService.shortenUrl(request);

        assertThat(response.getCustomAlias()).isNull();
        assertThat(response.getShortUrl()).endsWith("/" + response.getShortCode());
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
    }
}
