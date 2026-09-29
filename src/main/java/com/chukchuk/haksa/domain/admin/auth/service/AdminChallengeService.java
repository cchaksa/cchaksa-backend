package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminLoginChallenge;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminLoginChallengeRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
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
   * @return 저장된 challenge
   */
  @Transactional
  public AdminLoginChallenge issue() {
    Instant now = clock.instant();
    return repository.save(
        new AdminLoginChallenge(
            UUID.randomUUID(),
            tokenCodec.randomToken(),
            now.plus(properties.getChallengeTtl()),
            now));
  }

  /**
   * 로그인 challenge를 배타적으로 소모하고 원본 nonce를 반환한다.
   *
   * @param challengeId challenge 식별자
   * @return 서버가 저장한 nonce
   * @throws CommonException challenge가 없거나 만료·사용된 경우
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String consume(UUID challengeId) {
    Instant now = clock.instant();
    AdminLoginChallenge challenge =
        repository
            .findByIdForUpdate(challengeId)
            .orElseThrow(() -> new CommonException(ErrorCode.ADMIN_CHALLENGE_INVALID));
    if (!challenge.canUse(now)) {
      throw new CommonException(ErrorCode.ADMIN_CHALLENGE_INVALID);
    }
    challenge.consume(now);
    return challenge.getNonce();
  }
}
