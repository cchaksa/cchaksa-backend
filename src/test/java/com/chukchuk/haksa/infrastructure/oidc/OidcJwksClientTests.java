package com.chukchuk.haksa.infrastructure.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OidcJwksClientTests.TestConfig.class)
class OidcJwksClientTests {

    @Autowired
    private OidcJwksClient client;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CacheManager cacheManager;

    @AfterEach
    void clearCache() {
        cacheManager.getCache("oidcKeys").clear();
    }

    @Test
    void fetchKeysCachesResponseByKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode appleKeys = mapper.readTree("{\"keys\":[{\"kid\":\"1\"}]}" );
        JsonNode kakaoKeys = mapper.readTree("{\"keys\":[{\"kid\":\"2\"}]}" );

        when(restTemplate.getForObject("https://apple", JsonNode.class)).thenReturn(appleKeys);
        when(restTemplate.getForObject("https://kakao", JsonNode.class)).thenReturn(kakaoKeys);

        JsonNode firstApple = client.fetchKeys("apple", "https://apple");
        JsonNode secondApple = client.fetchKeys("apple", "https://apple");
        JsonNode kakao = client.fetchKeys("kakao", "https://kakao");

        assertThat(firstApple).isSameAs(secondApple);
        assertThat(kakao.get("keys").get(0).get("kid").asText()).isEqualTo("2");

        verify(restTemplate, times(1)).getForObject("https://apple", JsonNode.class);
        verify(restTemplate, times(1)).getForObject("https://kakao", JsonNode.class);
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("oidcKeys");
        }

        @Bean
        RestTemplate restTemplate() {
            return Mockito.mock(RestTemplate.class);
        }

        @Bean
        OidcJwksClient oidcJwksClient(RestTemplate restTemplate) {
            return new OidcJwksClient(restTemplate);
        }
    }
}
