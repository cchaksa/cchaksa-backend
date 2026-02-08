package com.chukchuk.haksa.infrastructure.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OidcJwksClient {

    private static final String CACHE_NAME = "oidcKeys";

    private final RestTemplate restTemplate;

    @Cacheable(cacheNames = CACHE_NAME, key = "#cacheKey")
    public JsonNode fetchKeys(String cacheKey, String url) {
        return restTemplate.getForObject(url, JsonNode.class);
    }
}
