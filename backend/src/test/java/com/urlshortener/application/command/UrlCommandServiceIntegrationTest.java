package com.urlshortener.application.command;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.security.ActorContext;
import org.junit.jupiter.api.Test;
import org.sqids.Sqids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UrlCommandServiceIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final ActorContext USER = new ActorContext(
            ActorContext.SubjectType.USER, USER_ID, null, "Test User", "user@example.com",
            null, Set.of(), Set.of("urls.read", "urls.write"));

    @Autowired
    private UrlCommandService commandService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private Sqids sqids;

    @Test
    void create_shouldPersistSqidAndImmutableOwner() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com/integration", null, null, null, USER);

        UrlResponse response = commandService.handle(command);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
        assertThat(sqids.decode(response.getShortCode())).containsExactly(response.getId());
        assertThat(urlRepository.findByShortCode(response.getShortCode()))
                .get()
                .extracting(url -> url.getOwnerUid())
                .isEqualTo(USER_ID);
    }
}
