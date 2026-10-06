package com.chukchuk.haksa.domain.user.service;

import io.jsonwebtoken.Claims;

/** OIDC ID 토큰의 서명·발급자·대상·nonce를 검증해 사용자 식별정보를 반환한다. */
public interface OidcService {
  /**
   * 입력 값과 업무 처리 조건을 검증한다.
   *
   * @param idToken ID token
   * @param nonce 재전송 공격 방지를 위해 ID 토큰과 대조할 nonce
   * @return claims
   */
  Claims verifyIdToken(String idToken, String nonce);
}
