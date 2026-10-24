package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void shortenUrl_shouldCreateNewShortUrl() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com/very/long/path");

        Url savedUrl = Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com/very/long/path")
                .shortCode("abc1234")
                .clickCount(0L)
                .isActive(true)
                .build();

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);

        UrlResponse response = urlService.shortenUrl(request);

        assertThat(response).isNotNull();
        assertThat(response.getOriginalUrl()).isEqualTo("https://www.example.com/very/long/path");
        assertThat(response.getShortCode()).isEqualTo("abc1234");
        assertThat(response.getShortUrl()).contains("abc1234");
        verify(urlRepository, times(1)).save(any(Url.class));
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
}
