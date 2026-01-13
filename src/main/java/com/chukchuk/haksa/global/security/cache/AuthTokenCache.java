package com.chukchuk.haksa.global.security.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class AuthTokenCache {

    private final Cache<String, UserDetails> cache;
    private final Cache<String, Set<String>> userTokenIndex;

    public AuthTokenCache(@Value("${security.jwt.access-expiration}") long accessExpirationMs) {
        Duration ttl = Duration.ofMillis(accessExpirationMs);
        this.cache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(ttl)
                .build();
        this.userTokenIndex = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(ttl)
                .build();
    }

    public UserDetails get(String tokenHash) {
        return cache.getIfPresent(tokenHash);
    }

    public void put(String userId, String tokenHash, UserDetails userDetails) {
        cache.put(tokenHash, userDetails);
        userTokenIndex.asMap().compute(userId, (key, existing) -> {
            Set<String> set = (existing != null) ? existing : ConcurrentHashMap.newKeySet();
            set.add(tokenHash);
            return set;
        });
    }

    public UserDetails getOrLoad(String userId, String token, Supplier<UserDetails> loader) {
        String tokenHash = hashToken(token);
        if ("hash_err".equals(tokenHash)) {
            return loader.get();
        }

        UserDetails cached = cache.getIfPresent(tokenHash);
        if (cached != null) {
            return cached;
        }

        UserDetails loaded = loader.get();
        put(userId, tokenHash, loaded);
        return loaded;
    }

    public void evictByUserId(String userId) {
        Set<String> tokenHashes = userTokenIndex.asMap().remove(userId);
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            cache.invalidateAll(tokenHashes);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return "hash_err";
        }
    }
}
