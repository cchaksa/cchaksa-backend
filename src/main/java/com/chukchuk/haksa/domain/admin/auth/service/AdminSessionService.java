package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.model.AdminSession;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminSessionRepository;
import com.chukchuk.haksa.domain.admin.auth.security.AdminPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 불투명 세션을 생성, 인증, 폐기한다. */
@Service
@RequiredArgsConstructor
public class AdminSessionService {
  private final AdminSessionRepository repository;
  private final AdminTokenCodec tokenCodec;
  private final AdminAuthProperties properties;
  private final Clock clock;

  /**
   * 관리자 계정에 새 불투명 세션을 발급한다.
   *
   * @param account 세션을 발급할 관리자
   * @return 클라이언트용 원문과 만료 시각
   */
  @Transactional
  public CreatedSession create(AdminAccount account) {
    Instant now = clock.instant();
    String rawToken = tokenCodec.randomToken();
    Instant absoluteExpiry = now.plus(properties.getAbsoluteTimeout());
    Instant idleExpiry = now.plus(properties.getIdleTimeout());
    if (idleExpiry.isAfter(absoluteExpiry)) {
      idleExpiry = absoluteExpiry;
    }
    AdminSession session =
        repository.save(
            new AdminSession(
                UUID.randomUUID(),
                account,
                tokenCodec.hash(rawToken),
                absoluteExpiry,
                idleExpiry,
                now));
    return new CreatedSession(rawToken, session.getExpiresAt());
  }

  /**
   * 쿠키 원문을 해시로 조회하고 유효한 관리자 principal을 반환한다.
   *
   * @param rawToken 쿠키 세션 원문
   * @return 유효한 관리자 principal
   */
  @Transactional
  public Optional<AdminPrincipal> authenticate(String rawToken) {
    Instant now = clock.instant();
    return repository
        .findByTokenHash(tokenCodec.hash(rawToken))
        .filter(session -> session.isUsable(now))
        .filter(session -> session.getAdminAccount().isActive())
        .map(
            session -> {
              session.touch(now, properties.getIdleTimeout());
              AdminAccount account = session.getAdminAccount();
              return new AdminPrincipal(
                  account.getId(),
                  session.getId(),
                  account.getDisplayName(),
                  account.getAdminRole());
            });
  }

  /**
   * 현재 세션을 폐기한다.
   *
   * @param rawToken 쿠키 세션 원문
   */
  @Transactional
  public void revoke(String rawToken) {
    repository
        .findByTokenHash(tokenCodec.hash(rawToken))
        .ifPresent(session -> session.revoke(clock.instant()));
  }

  /**
   * 한 관리자에게 발급된 모든 세션을 폐기한다.
   *
   * @param adminAccountId 관리자 계정 식별자
   * @return 폐기된 세션 수
   */
  @Transactional
  public int revokeAll(UUID adminAccountId) {
    return repository.revokeAllByAdminAccountId(adminAccountId, clock.instant());
  }

  /** 클라이언트에 전달할 세션 원문과 절대 만료 시각이다. */
  public record CreatedSession(String token, Instant expiresAt) {}
}
