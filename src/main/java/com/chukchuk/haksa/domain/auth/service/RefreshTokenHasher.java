// 리프레시 토큰 원문을 서버 비밀키 기반 HMAC 해시로 변환한다

package com.chukchuk.haksa.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Refresh token의 해시 생성과 일치 여부 검사를 담당한다. */
@Component
public class RefreshTokenHasher {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final byte[] secret;

  /**
   * 서버 비밀키로 리프레시 토큰 해시 생성기를 초기화한다.
   *
   * @param secret JWT 서명에 사용하는 서버 비밀키
   */
  public RefreshTokenHasher(@Value("${security.jwt.secret}") String secret) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * 리프레시 토큰 원문을 HMAC-SHA256으로 해시한다.
   *
   * @param refreshToken 해시할 리프레시 토큰 원문
   * @return URL-safe Base64로 인코딩한 토큰 해시
   * @throws IllegalStateException HMAC-SHA256 알고리즘을 사용할 수 없거나 비밀키가 유효하지 않은 경우
   */
  public String hash(String refreshToken) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
      byte[] digest = mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to hash refresh token", e);
    }
  }

  /**
   * 원문 refresh token이 저장된 해시와 일치하는지 확인한다.
   *
   * @param refreshToken refresh token 원문
   * @param tokenHash 원문 토큰 검증에 사용할 저장된 해시
   * @return 원문 토큰이 저장된 해시와 일치하면 {@code true}
   */
  public boolean matches(String refreshToken, String tokenHash) {
    if (refreshToken == null || tokenHash == null || tokenHash.isBlank()) {
      return false;
    }

    return MessageDigest.isEqual(
        hash(refreshToken).getBytes(StandardCharsets.UTF_8),
        tokenHash.getBytes(StandardCharsets.UTF_8));
  }
}
