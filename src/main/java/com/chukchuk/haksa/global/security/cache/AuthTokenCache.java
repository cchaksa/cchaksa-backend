package com.chukchuk.haksa.global.security.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthTokenCache {

    private final Cache<String, UserDetails> cache;

    public AuthTokenCache(@Value("${security.jwt.access-expiration}") long accessExpirationMs) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofMillis(accessExpirationMs))
                .build();
    }

    public UserDetails get(String tokenHash) {
        return cache.getIfPresent(tokenHash);
    }

    public void put(String tokenHash, UserDetails userDetails) {
        cache.put(tokenHash, userDetails);
    }
}
