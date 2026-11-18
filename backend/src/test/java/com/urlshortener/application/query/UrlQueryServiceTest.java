package com.urlshortener.application.query;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlQueryServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private UrlQueryService queryService;

    @BeforeEach
    void setUp() {
        UrlResponseMapper responseMapper = new UrlResponseMapper();
        ReflectionTestUtils.setField(responseMapper, "baseUrl", "http://localhost:8080");
        queryService = new UrlQueryService(urlRepository, responseMapper);
    }

    @Test
    void resolveShortCode_shouldReturnReadModelWithoutRecordingClick() {
        Url url = activeUrl();
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(url));

        ResolvedUrl result = queryService.handle(new ResolveShortCodeQuery("abc1234"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.originalUrl()).isEqualTo("https://www.example.com");
        verify(urlRepository).findByShortCode("abc1234");
        verify(urlRepository, never()).incrementClickCount(anyLong());
    }

    @Test
    void resolveShortCode_shouldThrowWhenNotFound() {
        when(urlRepository.findByShortCode("notexist")).thenReturn(Optional.empty());
        when(urlRepository.findByCustomAlias("notexist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.handle(new ResolveShortCodeQuery("notexist")))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void listUrls_shouldMapActiveUrls() {
        when(urlRepository.findAllActiveAndNonExpired()).thenReturn(List.of(activeUrl()));

        List<UrlResponse> result = queryService.handle(new ListUrlsQuery());

        assertThat(result).singleElement()
                .satisfies(url -> assertThat(url.getShortCode()).isEqualTo("abc1234"));
    }

    private Url activeUrl() {
        return Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .clickCount(0L)
                .isActive(true)
                .build();
    }
}
