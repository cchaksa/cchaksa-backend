package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.infrastructure.oidc.kakao.KakaoIdTokenVerifier;
import io.jsonwebtoken.Claims;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 관리자용 Kakao 앱 audience로 ID Token을 검증한다. */
@Service
@RequiredArgsConstructor
public class AdminKakaoOidcService {
  private final AdminKakaoTokenClient tokenClient;
  private final KakaoIdTokenVerifier verifier;
  private final AdminAuthProperties properties;

  /**
   * Kakao 인가 코드를 교환하고 관리자 JavaScript 앱 audience와 서버 nonce를 검증한다.
   *
   * @param authorizationCode Kakao authorization code
   * @param nonce 서버가 보관한 nonce
   * @return 검증된 claim
   */
  public Claims exchangeAndVerify(String authorizationCode, String nonce) {
    String idToken = tokenClient.exchange(authorizationCode);
    AdminAuthProperties.Kakao kakao = properties.requireKakaoLoginConfiguration();
    return verifier.verify(idToken, nonce, Set.of(kakao.getJavascriptAppKey()));
  }
}
