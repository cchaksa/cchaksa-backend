package com.chukchuk.haksa.infrastructure.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** OIDC 제공자의 JWKS를 조회하고 서명 키를 선택한다. */
@Component
@RequiredArgsConstructor
public class OidcJwksClient {

  private static final String CACHE_NAME = "oidcKeys";

  private final RestTemplate restTemplate;
  private final CacheManager cacheManager;

  /**
   * OIDC 제공자의 JWKS를 조회하고 제공자별 key로 캐시한다.
   *
   * @param cacheKey OIDC 제공자를 구분할 캐시 key
   * @param url JWKS를 제공하는 HTTPS URL
   * @return 캐시에 있거나 원격 제공자에서 조회한 JWKS JSON
   */
  @Cacheable(cacheNames = CACHE_NAME, key = "#cacheKey")
  public JsonNode fetchKeys(String cacheKey, String url) {
    return restTemplate.getForObject(url, JsonNode.class);
  }

  /**
   * OIDC 제공자의 JWKS를 다시 조회해 기존 캐시 값을 교체한다.
   *
   * @param cacheKey 교체할 OIDC 제공자 캐시 key
   * @param url JWKS를 제공하는 HTTPS URL
   * @return 원격 제공자에서 새로 조회한 JWKS JSON
   */
  public JsonNode refreshKeys(String cacheKey, String url) {
    JsonNode keys = restTemplate.getForObject(url, JsonNode.class);
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache != null) {
      cache.put(cacheKey, keys);
    }
    return keys;
  }
}
