package com.chukchuk.haksa.global.security.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** 사용자별 유효 인증 토큰 해시를 캐시한다. */
@Component
public class AuthTokenCache {

  private final Cache<String, UserDetails> cache;
  private final Cache<String, Set<String>> userTokenIndex;

  /**
   * 액세스 토큰 만료 시간과 같은 TTL을 사용하는 사용자 인증 캐시를 생성한다.
   *
   * @param accessExpirationMs 캐시 항목에 적용할 액세스 토큰 만료 시간(밀리초)
   */
  public AuthTokenCache(@Value("${security.jwt.access-expiration}") long accessExpirationMs) {
    Duration ttl = Duration.ofMillis(accessExpirationMs);
    this.cache = Caffeine.newBuilder().maximumSize(50_000).expireAfterWrite(ttl).build();
    this.userTokenIndex = Caffeine.newBuilder().maximumSize(50_000).expireAfterWrite(ttl).build();
  }

  /**
   * 토큰 해시에 대응하는 사용자 인증 정보를 캐시에서 조회한다.
   *
   * @param tokenHash 원본 토큰의 SHA-256 해시
   * @return 캐시된 사용자 인증 정보이며 없으면 {@code null}
   */
  public UserDetails get(String tokenHash) {
    return cache.getIfPresent(tokenHash);
  }

  /**
   * 토큰으로 사용자 인증 정보를 조회하고 캐시 miss이면 공급자를 한 번 실행해 저장한다.
   *
   * @param userId 사용자 식별자
   * @param token 해시로 변환해 캐시 key로 사용할 원본 access token
   * @param loader 캐시 miss일 때 사용자 인증 정보를 조회할 공급자
   * @return 캐시에 있거나 공급자가 새로 조회한 사용자 인증 정보
   */
  public UserDetails getOrLoad(String userId, String token, Supplier<UserDetails> loader) {
    String tokenHash = hashToken(token);
    return cache.get(
        tokenHash,
        key -> {
          UserDetails loaded = loader.get();
          recordTokenHash(userId, key);
          return loaded;
        });
  }

  /**
   * 사용자에게 발급된 인증 토큰 캐시를 제거한다.
   *
   * @param userId 사용자 식별자
   */
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
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  private void recordTokenHash(String userId, String tokenHash) {
    userTokenIndex
        .asMap()
        .compute(
            userId,
            (key, existing) -> {
              Set<String> set = (existing != null) ? existing : ConcurrentHashMap.newKeySet();
              set.add(tokenHash);
              return set;
            });
  }
}
