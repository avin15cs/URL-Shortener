package com.urlshortener.config;

import org.sqids.Sqids;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Configuration
public class SqidsConfig {

    private static final int MINIMUM_SHORT_CODE_LENGTH = 7;

    @Bean
    public Sqids sqids(AppProperties appProperties) {
        Set<String> blockList = new HashSet<>(Sqids.Builder.DEFAULT_BLOCK_LIST);
        if (appProperties.getReservedPaths() != null) {
            appProperties.getReservedPaths().stream()
                    .map(path -> path.toLowerCase(Locale.ROOT))
                    .forEach(blockList::add);
        }

        return Sqids.builder()
                .minLength(MINIMUM_SHORT_CODE_LENGTH)
                .blockList(blockList)
                .build();
    }
}
