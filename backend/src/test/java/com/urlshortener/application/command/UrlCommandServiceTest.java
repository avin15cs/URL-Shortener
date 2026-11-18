package com.urlshortener.application.command;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sqids.Sqids;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlCommandServiceTest {

    @Mock
    private UrlRepository urlRepository;

    private UrlCommandService commandService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setReservedPaths(Set.of("actuator", "api", "v3"));

        UrlResponseMapper responseMapper = new UrlResponseMapper();
        ReflectionTestUtils.setField(responseMapper, "baseUrl", "http://localhost:8080");

        commandService = new UrlCommandService(
                urlRepository,
                appProperties,
                Sqids.builder().minLength(7).build(),
                responseMapper);
    }

    @Test
    void create_shouldCreateNewShortUrl() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com/very/long/path", null, null, null);

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.existsByCustomAlias(anyString())).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
            Url savedUrl = invocation.getArgument(0);
            if (savedUrl.getId() == null) {
                savedUrl.setId(1L);
            }
            return savedUrl;
        });

        UrlResponse response = commandService.handle(command);

        assertThat(response.getOriginalUrl()).isEqualTo(command.originalUrl());
        assertThat(response.getShortCode()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(response.getShortUrl()).endsWith("/" + response.getShortCode());
        assertThat(Sqids.builder().minLength(7).build().decode(response.getShortCode()))
                .containsExactly(1L);
        verify(urlRepository, times(2)).saveAndFlush(any(Url.class));
    }

    @Test
    void deactivate_shouldSoftDeleteUrl() {
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .isActive(true)
                .clickCount(0L)
                .build();

        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        commandService.handle(new DeactivateUrlCommand(1L));

        verify(urlRepository).save(argThat(saved -> !saved.getIsActive()));
    }

    @Test
    void recordClick_shouldIncrementAtomically() {
        commandService.handle(new RecordClickCommand(1L));

        verify(urlRepository).incrementClickCount(1L);
    }

    @Test
    void create_shouldRejectReservedAlias() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com", "actuator", null, null);

        assertThatThrownBy(() -> commandService.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved keyword");
    }

    @Test
    void create_shouldStoreCustomAliasAsCanonicalShortCode() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com", "my-link", null, null);

        when(urlRepository.existsByShortCode("my-link")).thenReturn(false);
        when(urlRepository.existsByCustomAlias("my-link")).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponse response = commandService.handle(command);

        assertThat(response.getShortCode()).isEqualTo("my-link");
        assertThat(response.getCustomAlias()).isEqualTo("my-link");
        assertThat(response.getShortUrl()).endsWith("/my-link");
        verify(urlRepository).saveAndFlush(
                argThat(url -> "my-link".equals(url.getShortCode())));
    }

    @Test
    void create_shouldTreatBlankAliasAsAbsent() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com", "", null, null);

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.existsByCustomAlias(anyString())).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> {
            Url savedUrl = invocation.getArgument(0);
            savedUrl.setId(2L);
            return savedUrl;
        });

        UrlResponse response = commandService.handle(command);

        assertThat(response.getCustomAlias()).isNull();
        assertThat(response.getShortUrl()).endsWith("/" + response.getShortCode());
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
    }
}
