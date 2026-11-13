package com.urlshortener.application.query;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.security.ActorContext;
import com.urlshortener.security.UrlAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlQueryServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ActorContext USER = new ActorContext(
            ActorContext.SubjectType.USER, USER_ID, null, "Test User", null, null,
            Set.of(), Set.of("urls.read"));
    private static final ActorContext ADMIN = new ActorContext(
            ActorContext.SubjectType.USER,
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            null, "Admin", null, null, Set.of(ActorContext.URL_ADMIN), Set.of("urls.read"));
    private static final ActorContext CLIENT = new ActorContext(
            ActorContext.SubjectType.CLIENT, null, "url-shortener-automation", "Automation",
            null, null, Set.of(), Set.of("urls.read"));

    @Mock
    private UrlRepository urlRepository;

    private UrlQueryService queryService;

    @BeforeEach
    void setUp() {
        UrlResponseMapper responseMapper = new UrlResponseMapper();
        ReflectionTestUtils.setField(responseMapper, "baseUrl", "http://localhost:8080");
        queryService = new UrlQueryService(urlRepository, responseMapper, new UrlAccessPolicy());
    }

    @Test
    void resolveShortCode_shouldReturnPublicReadModelWithoutRecordingClick() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(activeUrl(USER_ID)));

        ResolvedUrl result = queryService.handle(new ResolveShortCodeQuery("abc1234"));

        assertThat(result.originalUrl()).isEqualTo("https://www.example.com");
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
    void listUrls_shouldFetchOnlyCurrentUsersUrls() {
        when(urlRepository.findAllActiveAndNonExpiredByOwnerUid(USER_ID))
                .thenReturn(List.of(activeUrl(USER_ID)));

        List<UrlResponse> result = queryService.handle(new ListUrlsQuery(USER));

        assertThat(result).singleElement();
        verify(urlRepository).findAllActiveAndNonExpiredByOwnerUid(USER_ID);
        verify(urlRepository, never()).findAllActiveAndNonExpired();
    }

    @Test
    void listUrls_shouldFetchAllUrlsForAdmin() {
        when(urlRepository.findAllActiveAndNonExpired()).thenReturn(List.of(activeUrl(USER_ID)));

        assertThat(queryService.handle(new ListUrlsQuery(ADMIN))).hasSize(1);

        verify(urlRepository).findAllActiveAndNonExpired();
    }

    @Test
    void listUrls_shouldFetchOnlyCurrentClientsUrls() {
        when(urlRepository.findAllActiveAndNonExpiredByOwnerClientId("url-shortener-automation"))
                .thenReturn(List.of(activeUrlForClient("url-shortener-automation")));

        assertThat(queryService.handle(new ListUrlsQuery(CLIENT))).hasSize(1);

        verify(urlRepository).findAllActiveAndNonExpiredByOwnerClientId("url-shortener-automation");
        verify(urlRepository, never()).findAllActiveAndNonExpired();
    }

    @Test
    void listUrls_shouldRequireReadScope() {
        ActorContext actorWithoutReadScope = new ActorContext(
                ActorContext.SubjectType.USER, USER_ID, null, "Test User", null, null,
                Set.of(), Set.of("urls.write"));

        assertThatThrownBy(() -> queryService.handle(new ListUrlsQuery(actorWithoutReadScope)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("urls.read");
    }

    @Test
    void getUrlById_shouldRejectAnotherOwner() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(activeUrl(
                UUID.fromString("33333333-3333-3333-3333-333333333333"))));

        assertThatThrownBy(() -> queryService.handle(new GetUrlByIdQuery(1L, USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Url activeUrl(UUID ownerUid) {
        return Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .clickCount(0L)
                .isActive(true)
                .ownerUid(ownerUid)
                .build();
    }

    private Url activeUrlForClient(String clientId) {
        return Url.builder()
                .id(2L)
                .originalUrl("https://www.example.com/automation")
                .shortCode("def5678")
                .clickCount(0L)
                .isActive(true)
                .ownerClientId(clientId)
                .build();
    }
}
