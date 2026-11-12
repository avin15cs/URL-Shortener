package com.urlshortener.service;

import com.urlshortener.dto.ShortenUrlRequest;
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
class UrlServiceIntegrationTest {

    @Autowired
    private UrlService urlService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private Sqids sqids;

    @Test
    void shortenUrl_shouldReplacePendingCodeWithSqidBeforeReturning() {
        ShortenUrlRequest request = new ShortenUrlRequest();
        request.setOriginalUrl("https://www.example.com/integration");

        UrlResponse response = urlService.shortenUrl(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getShortCode()).doesNotStartWith("~pending_");
        assertThat(sqids.decode(response.getShortCode())).containsExactly(response.getId());
        assertThat(urlRepository.findByShortCode(response.getShortCode())).isPresent();
    }
}
