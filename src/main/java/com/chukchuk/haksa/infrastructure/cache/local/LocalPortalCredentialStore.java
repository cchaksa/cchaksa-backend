package com.chukchuk.haksa.infrastructure.cache.local;

import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.global.crypto.AesEncryptor;
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

    private final AesEncryptor aesEncryptor;

    private final Cache<String, EncryptedCredential> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(TTL)
                    .maximumSize(5_000)
                    .build();

    @Override
    public void save(String userId, String username, String password) {
        try {
            cache.put(
                    key(userId),
                    new EncryptedCredential(
                            aesEncryptor.encrypt(username),
                            aesEncryptor.encrypt(password)
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("포털 자격증명 저장 실패", e);
        }
    }

    @Override
    public String getUsername(String userId) {
        EncryptedCredential c = cache.getIfPresent(key(userId));
        if (c == null) return null;
        try {
            return aesEncryptor.decrypt(c.username());
        } catch (Exception e) {
            throw new RuntimeException("username 복호화 실패", e);
        }
    }

    @Override
    public String getPassword(String userId) {
        EncryptedCredential c = cache.getIfPresent(key(userId));
        if (c == null) return null;
        try {
            return aesEncryptor.decrypt(c.password());
        } catch (Exception e) {
            throw new RuntimeException("password 복호화 실패", e);
        }
    }

    @Override
    public void clear(String userId) {
        cache.invalidate(key(userId));
    }

    private String key(String userId) {
        return "portal:" + userId;
    }

    private record EncryptedCredential(String username, String password) {}
}