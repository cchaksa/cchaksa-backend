package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminLoginChallenge;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminLoginChallengeRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 서버 보관형 일회성 OIDC nonce를 발급하고 소모한다. */
@Service
@RequiredArgsConstructor
public class AdminChallengeService {
  private final AdminLoginChallengeRepository repository;
  private final AdminTokenCodec tokenCodec;
  private final AdminAuthProperties properties;
  private final Clock clock;

  /**
   * 새 일회성 challenge를 발급한다.
   *
   * @return 저장된 challenge와 브라우저 결합 token
   */
  @Transactional
  public IssuedChallenge issue() {
    Instant now = clock.instant();
    String browserToken = tokenCodec.randomToken();
    AdminLoginChallenge challenge =
        repository.save(
            new AdminLoginChallenge(
                UUID.randomUUID(),
                tokenCodec.randomToken(),
                tokenCodec.randomToken(),
                tokenCodec.hash(browserToken),
                now.plus(properties.getChallengeTtl()),
                now));
    return new IssuedChallenge(challenge, browserToken);
  }

  /**
   * 로그인 challenge를 배타적으로 소모하고 원본 nonce를 반환한다.
   *
   * @param challengeId challenge 식별자
   * @param state Kakao authorization 응답으로 돌아온 state
   * @param browserToken 로그인 시작 브라우저의 HttpOnly cookie 값
   * @return 서버가 저장한 nonce
   * @throws CommonException challenge가 없거나 만료·사용된 경우
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String consume(UUID challengeId, String state, String browserToken) {
    Instant now = clock.instant();
    AdminLoginChallenge challenge =
        repository
            .findByIdForUpdate(challengeId)
            .orElseThrow(() -> new CommonException(ErrorCode.ADMIN_CHALLENGE_INVALID));
    if (!challenge.canUse(now)
        || !matches(challenge.getState(), state)
        || !matchesBrowser(challenge.getBrowserTokenHash(), browserToken)) {
      throw new CommonException(ErrorCode.ADMIN_CHALLENGE_INVALID);
    }
    challenge.consume(now);
    return challenge.getNonce();
  }

  private boolean matches(String expected, String actual) {
    if (actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }

  private boolean matchesBrowser(String expectedHash, String browserToken) {
    if (browserToken == null) {
      return false;
    }
    return matches(expectedHash, tokenCodec.hash(browserToken));
  }

  /** 로그인 시작 브라우저에 전달할 원문 token과 DB challenge를 분리한다. */
  public record IssuedChallenge(AdminLoginChallenge challenge, String browserToken) {}
}
