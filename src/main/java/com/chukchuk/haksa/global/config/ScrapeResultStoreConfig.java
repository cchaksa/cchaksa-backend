package com.chukchuk.haksa.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** 스크래핑 결과 저장소의 region과 호출 제한 시간을 적용한 S3 클라이언트를 구성한다. */
@Configuration
@RequiredArgsConstructor
public class ScrapeResultStoreConfig {

  private final ScrapingProperties scrapingProperties;

  /**
   * 스크래핑 결과 저장소에 접근할 S3 클라이언트를 생성한다.
   *
   * @return 결과 저장소 설정이 적용된 동기 S3 클라이언트
   */
  @Bean
  public S3Client scrapeResultStoreS3Client() {
    ScrapingProperties.ResultStore store = scrapingProperties.getResultStore();
    return S3Client.builder()
        .region(Region.of(store.getRegion()))
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .apiCallTimeout(java.time.Duration.ofSeconds(store.getApiCallTimeoutSeconds()))
                .apiCallAttemptTimeout(
                    java.time.Duration.ofSeconds(store.getApiCallAttemptTimeoutSeconds()))
                .build())
        .build();
  }
}
