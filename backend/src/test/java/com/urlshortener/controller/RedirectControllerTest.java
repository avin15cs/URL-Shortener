package com.urlshortener.controller;

import com.urlshortener.application.command.RecordClickCommand;
import com.urlshortener.application.command.UrlCommandService;
import com.urlshortener.application.query.ResolveShortCodeQuery;
import com.urlshortener.application.query.ResolvedUrl;
import com.urlshortener.application.query.UrlQueryService;
import com.urlshortener.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectControllerTest {

    @Mock
    private UrlCommandService commandService;

    @Mock
    private UrlQueryService queryService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private RedirectController controller;

    @Test
    void redirect_shouldResolveQueryThenRecordClickCommand() {
        when(queryService.handle(new ResolveShortCodeQuery("abc1234")))
                .thenReturn(new ResolvedUrl(1L, "https://www.example.com"));

        ResponseEntity<Void> response = controller.redirect("abc1234");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://www.example.com"));
        verify(queryService).handle(new ResolveShortCodeQuery("abc1234"));
        verify(commandService).handle(new RecordClickCommand(1L));
    }
}
