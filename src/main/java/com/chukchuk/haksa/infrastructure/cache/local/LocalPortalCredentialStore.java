package com.chukchuk.haksa.infrastructure.cache.local;

import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 비동기 포털 연동에 필요한 자격 증명을 임시 보관한다. */
@Component
@RequiredArgsConstructor
public class LocalPortalCredentialStore implements PortalCredentialStore {

  private static final Duration TTL = Duration.ofMinutes(10);

  private final Cache<String, Credential> cache =
      Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(5_000).build();

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
