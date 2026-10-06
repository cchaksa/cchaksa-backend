package com.chukchuk.haksa.global.logging.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** 로그 식별용 단방향 해시 함수를 제공한다. */
public final class HashUtil {
  private HashUtil() {}

  /**
   * 입력값의 축약 SHA-256 해시를 반환한다.
   *
   * @param input 로그에서 원문 대신 식별할 문자열
   * @return SHA-256 결과 앞부분을 URL-safe Base64 10자로 표현한 값이며 실패하면 {@code hash_err}
   */
  public static String sha256Short(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
      // 6~8바이트 정도로 축약
      return Base64.getUrlEncoder().withoutPadding().encodeToString(d).substring(0, 10);
    } catch (Exception e) {
      return "hash_err";
    }
  }
}
