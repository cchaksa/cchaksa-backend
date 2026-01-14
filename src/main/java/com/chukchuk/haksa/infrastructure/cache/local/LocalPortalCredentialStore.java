package com.chukchuk.haksa.infrastructure.cache.local;

import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "portal.credential.store",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalPortalCredentialStore implements PortalCredentialStore {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Cache<String, Credential> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(TTL)
                    .maximumSize(5_000)
                    .build();

    @Override
    public void save(String userId, String username, String password) {
        cache.put(key(userId), new Credential(username, password));
    }

    @Override
    public String getUsername(String userId) {
        Credential c = cache.getIfPresent(key(userId));
        return c == null ? null : c.username();
    }

    @Override
    public String getPassword(String userId) {
        Credential c = cache.getIfPresent(key(userId));
        return c == null ? null : c.password();
    }

    @Override
    public void clear(String userId) {
        cache.invalidate(key(userId));
    }

    private String key(String userId) {
        return "portal:" + userId;
    }

    private record Credential(String username, String password) {}
}