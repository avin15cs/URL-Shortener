package com.urlshortener.application.command;

import com.urlshortener.dto.UrlResponse;
import com.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.sqids.Sqids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UrlCommandServiceIntegrationTest {

    @Autowired
    private UrlCommandService commandService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private Sqids sqids;

    @Test
    void create_shouldReplacePendingCodeWithSqidBeforeReturning() {
        CreateShortUrlCommand command = new CreateShortUrlCommand(
                "https://www.example.com/integration", null, null, null);

        UrlResponse response = commandService.handle(command);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
        assertThat(sqids.decode(response.getShortCode())).containsExactly(response.getId());
        assertThat(urlRepository.findByShortCode(response.getShortCode())).isPresent();
    }
}
