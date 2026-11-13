package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.security.ActorContext;
import org.sqids.Sqids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import com.urlshortener.config.AppProperties;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ActorContext USER = new ActorContext(
            ActorContext.SubjectType.USER,
            USER_ID,
            null,
            "Test User",
            "user@example.com",
            null,
            Set.of(),
            Set.of("urls.read", "urls.write"));
    private static final ActorContext ADMIN = new ActorContext(
            ActorContext.SubjectType.USER,
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            null,
            "Admin User",
            "admin@example.com",
            null,
            Set.of(ActorContext.URL_ADMIN),
            Set.of("urls.read", "urls.write"));

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

        UrlResponse response = urlService.shortenUrl(request, USER);

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
                .ownerUid(USER_ID)
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
                .ownerUid(USER_ID)
                .build();

        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        urlService.deleteUrl(1L, USER);

        verify(urlRepository).save(argThat(u -> !u.getIsActive()));
    }

    @Test
    void shortenUrl_shouldThrowWhenAliasIsReserved() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("actuator");

        assertThatThrownBy(() -> urlService.shortenUrl(request, USER))
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

        UrlResponse response = urlService.shortenUrl(request, USER);

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

        UrlResponse response = urlService.shortenUrl(request, USER);

        assertThat(response.getCustomAlias()).isNull();
        assertThat(response.getShortUrl()).endsWith("/" + response.getShortCode());
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
    }

    @Test
    void getAllUrls_shouldFetchOnlyCurrentUsersUrls() {
        when(urlRepository.findAllActiveAndNonExpiredByOwnerUid(USER_ID)).thenReturn(List.of());

        assertThat(urlService.getAllUrls(USER)).isEmpty();

        verify(urlRepository).findAllActiveAndNonExpiredByOwnerUid(USER_ID);
        verify(urlRepository, never()).findAllActiveAndNonExpired();
    }

    @Test
    void getAllUrls_shouldFetchAllUrlsForAdmin() {
        when(urlRepository.findAllActiveAndNonExpired()).thenReturn(List.of());

        assertThat(urlService.getAllUrls(ADMIN)).isEmpty();

        verify(urlRepository).findAllActiveAndNonExpired();
    }

    @Test
    void getUrlById_shouldRejectAnotherOwner() {
        Url url = Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .ownerUid(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .build();
        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> urlService.getUrlById(1L, USER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shortenUrl_shouldRequireWriteScope() {
        ActorContext readOnlyUser = new ActorContext(
                ActorContext.SubjectType.USER, USER_ID, null, "Test User", null, null,
                Set.of(), Set.of("urls.read"));
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        assertThatThrownBy(() -> urlService.shortenUrl(request, readOnlyUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("urls.write");
    }
}
