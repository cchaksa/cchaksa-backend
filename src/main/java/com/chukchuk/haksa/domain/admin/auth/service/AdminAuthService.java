package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminAccountRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import io.jsonwebtoken.Claims;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 challenge 검증, 허용 목록 조회와 세션 발급을 조정한다. */
@Service
@RequiredArgsConstructor
public class AdminAuthService {
  private final AdminChallengeService challengeService;
  private final AdminKakaoOidcService oidcService;
  private final AdminAccountRepository accountRepository;
  private final AdminSessionService sessionService;
  private final Clock clock;

  /**
   * 로그인 challenge와 ID Token을 검증해 관리자 세션을 발급한다.
   *
   * @param challengeId 서버 challenge 식별자
   * @param idToken Kakao ID Token
   * @return 로그인한 계정과 새 세션
   */
  @Transactional
  public SignInResult signIn(UUID challengeId, String idToken) {
    String nonce = challengeService.consume(challengeId);
    Claims claims = oidcService.verify(idToken, nonce);
    AdminAccount account =
        accountRepository
            .findByProviderAndSocialId("KAKAO", claims.getSubject())
            .filter(AdminAccount::isActive)
            .orElseThrow(() -> new CommonException(ErrorCode.ADMIN_LOGIN_DENIED));
    account.recordLogin(clock.instant());
    AdminSessionService.CreatedSession session = sessionService.create(account);
    return new SignInResult(account, session);
  }

  /** 로그인한 관리자 계정과 새 세션을 함께 반환한다. */
  public record SignInResult(AdminAccount account, AdminSessionService.CreatedSession session) {}
}
