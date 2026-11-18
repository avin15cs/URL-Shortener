package com.urlshortener.application.command;

import com.urlshortener.application.mapper.UrlResponseMapper;
import com.urlshortener.config.AppProperties;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.security.ActorContext;
import com.urlshortener.security.UrlAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sqids.Sqids;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ActorContext USER = user(USER_ID, Set.of("urls.read", "urls.write"));

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
                responseMapper,
                new UrlAccessPolicy());
    }

    @Test
    void create_shouldCreateOwnedShortUrl() {
        CreateShortUrlCommand command = createCommand(null, USER);
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

        assertThat(response.getShortCode()).hasSizeGreaterThanOrEqualTo(7);
        assertThat(Sqids.builder().minLength(7).build().decode(response.getShortCode()))
                .containsExactly(1L);
        verify(urlRepository, times(2)).saveAndFlush(
                argThat(url -> USER_ID.equals(url.getOwnerUid()) && url.getOwnerClientId() == null));
    }

    @Test
    void create_shouldStoreClientOwnership() {
        ActorContext client = new ActorContext(
                ActorContext.SubjectType.CLIENT, null, "url-shortener-automation",
                "url-shortener-automation", null, null, Set.of(), Set.of("urls.write"));
        when(urlRepository.existsByShortCode("machine-link")).thenReturn(false);
        when(urlRepository.existsByCustomAlias("machine-link")).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commandService.handle(createCommand("machine-link", client));

        verify(urlRepository).saveAndFlush(argThat(url ->
                url.getOwnerUid() == null
                        && "url-shortener-automation".equals(url.getOwnerClientId())));
    }

    @Test
    void deactivate_shouldSoftDeleteOwnedUrl() {
        Url url = ownedUrl(USER_ID);
        when(urlRepository.findById(1L)).thenReturn(Optional.of(url));
        when(urlRepository.save(any(Url.class))).thenReturn(url);

        commandService.handle(new DeactivateUrlCommand(1L, USER));

        verify(urlRepository).save(argThat(saved -> !saved.getIsActive()));
    }

    @Test
    void deactivate_shouldRejectAnotherOwner() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(ownedUrl(
                UUID.fromString("33333333-3333-3333-3333-333333333333"))));

        assertThatThrownBy(() -> commandService.handle(new DeactivateUrlCommand(1L, USER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void recordClick_shouldIncrementAtomicallyWithoutManagementAuthentication() {
        commandService.handle(new RecordClickCommand(1L));

        verify(urlRepository).incrementClickCount(1L);
    }

    @Test
    void create_shouldRejectReservedAlias() {
        assertThatThrownBy(() -> commandService.handle(createCommand("actuator", USER)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved keyword");
    }

    @Test
    void create_shouldStoreCustomAliasAsCanonicalShortCode() {
        when(urlRepository.existsByShortCode("my-link")).thenReturn(false);
        when(urlRepository.existsByCustomAlias("my-link")).thenReturn(false);
        when(urlRepository.saveAndFlush(any(Url.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponse response = commandService.handle(createCommand("my-link", USER));

        assertThat(response.getShortCode()).isEqualTo("my-link");
        assertThat(response.getCustomAlias()).isEqualTo("my-link");
    }

    @Test
    void create_shouldRequireWriteScope() {
        ActorContext readOnlyUser = user(USER_ID, Set.of("urls.read"));

        assertThatThrownBy(() -> commandService.handle(createCommand(null, readOnlyUser)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("urls.write");
    }

    private CreateShortUrlCommand createCommand(String alias, ActorContext actor) {
        return new CreateShortUrlCommand(
                "https://www.example.com/very/long/path", alias, null, null, actor);
    }

    private Url ownedUrl(UUID ownerUid) {
        return Url.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("abc1234")
                .isActive(true)
                .clickCount(0L)
                .ownerUid(ownerUid)
                .build();
    }

    private static ActorContext user(UUID id, Set<String> scopes) {
        return new ActorContext(
                ActorContext.SubjectType.USER, id, null, "Test User", "user@example.com",
                null, Set.of(), scopes);
    }
}
