package com.chukchuk.haksa.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class ScrapeJobDispatchConfig {

    private final ScrapingProperties scrapingProperties;

    @Bean
    public S3Client scrapeResultS3Client() {
        ScrapingProperties.ResultStore store = scrapingProperties.getResultStore();
        return S3Client.builder()
                .region(Region.of(store.getRegion()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(java.time.Duration.ofSeconds(store.getApiCallTimeoutSeconds()))
                        .apiCallAttemptTimeout(java.time.Duration.ofSeconds(store.getApiCallAttemptTimeoutSeconds()))
                        .build())
                .build();
    }
}
