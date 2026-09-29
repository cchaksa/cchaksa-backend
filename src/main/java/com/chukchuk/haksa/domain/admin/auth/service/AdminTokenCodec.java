package com.chukchuk.haksa.domain.admin.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 관리자 challenge와 세션에 사용할 난수 생성과 SHA-256 해시를 제공한다. */
@Component
public class AdminTokenCodec {
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * 256-bit URL-safe 난수를 생성한다.
   *
   * @return padding 없는 난수 문자열
   */
  public String randomToken() {
    byte[] value = new byte[32];
    secureRandom.nextBytes(value);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  /**
   * 값의 SHA-256 16진수 해시를 반환한다.
   *
   * @param value 해시할 원문
   * @return SHA-256 16진수 문자열
   * @throws IllegalStateException 런타임에 SHA-256을 사용할 수 없는 경우
   */
  public String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
