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
  private final KakaoIdTokenVerifier verifier;
  private final AdminAuthProperties properties;

  /**
   * 관리자 앱 audience와 서버 nonce로 ID Token을 검증한다.
   *
   * @param idToken Kakao ID Token
   * @param nonce 서버가 보관한 nonce
   * @return 검증된 claim
   */
  public Claims verify(String idToken, String nonce) {
    return verifier.verify(idToken, nonce, Set.of(properties.getKakao().getAppKey()));
  }
}
