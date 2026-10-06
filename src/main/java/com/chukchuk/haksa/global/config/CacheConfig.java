package com.chukchuk.haksa.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OIDC 공개키를 애플리케이션 메모리에 제한적으로 보관하는 Caffeine 캐시를 구성한다. */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final String OIDC_KEYS_CACHE = "oidcKeys";

  /**
   * 로컬 캐시 정책이 적용된 캐시 관리자를 생성한다.
   *
   * @return OIDC 공개키를 최대 10건, 한 시간 동안 보관하는 캐시 관리자
   */
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager(OIDC_KEYS_CACHE);
    cacheManager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(Duration.ofHours(1)).maximumSize(10));
    return cacheManager;
  }
}
