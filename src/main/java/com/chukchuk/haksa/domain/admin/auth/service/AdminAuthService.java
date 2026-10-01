package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminAccountRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 로컬 자격증명 검증과 비밀번호 변경을 조정한다. */
@Service
public class AdminAuthService {
  private final AdminAccountRepository accountRepository;
  private final AdminSessionService sessionService;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;
  private final String dummyPasswordHash;

  /**
   * 의존성을 구성하고 미등록 계정 timing 완화용 일회성 dummy hash를 생성한다.
   *
   * @param accountRepository 관리자 계정 저장소
   * @param sessionService 관리자 DB session 서비스
   * @param passwordEncoder BCrypt encoder
   * @param clock 인증 시각 기준
   */
  public AdminAuthService(
      AdminAccountRepository accountRepository,
      AdminSessionService sessionService,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.accountRepository = accountRepository;
    this.sessionService = sessionService;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
    this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  /**
   * 로컬 자격증명을 검증해 관리자 세션을 발급한다.
   *
   * @param loginId 관리자 login ID
   * @param password 비밀번호 원문
   * @return 로그인한 계정과 새 세션
   * @throws CommonException 자격증명이 올바르지 않거나 계정이 비활성인 경우
   */
  @Transactional
  public SignInResult signIn(String loginId, String password) {
    AdminAccount account = accountRepository.findByLoginId(loginId).orElse(null);
    String passwordHash = account == null ? dummyPasswordHash : account.getPasswordHash();
    boolean passwordMatches = passwordEncoder.matches(password, passwordHash);

    if (account == null || !passwordMatches || !account.isActive()) {
      throw new CommonException(ErrorCode.ADMIN_CREDENTIALS_INVALID);
    }

    account.recordLogin(clock.instant());
    return new SignInResult(account, sessionService.create(account));
  }

  /**
   * 현재 비밀번호를 검증하고 모든 기존 세션을 새 현재 세션으로 교체한다.
   *
   * @param adminAccountId 인증된 관리자 UUID
   * @param currentPassword 현재 비밀번호 원문
   * @param newPassword 새 비밀번호 원문
   * @return 현재 브라우저에 발급할 새 세션
   * @throws CommonException 현재 비밀번호가 다르거나 새 비밀번호가 기존과 같은 경우
   */
  @Transactional
  public AdminSessionService.CreatedSession changePassword(
      UUID adminAccountId, String currentPassword, String newPassword) {
    AdminAccount account =
        accountRepository
            .findByIdForUpdate(adminAccountId)
            .filter(AdminAccount::isActive)
            .orElseThrow(() -> new CommonException(ErrorCode.AUTHENTICATION_REQUIRED));

    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      throw new CommonException(ErrorCode.ADMIN_CURRENT_PASSWORD_MISMATCH);
    }
    if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
      throw new CommonException(ErrorCode.ADMIN_PASSWORD_UNCHANGED);
    }

    account.changePasswordHash(passwordEncoder.encode(newPassword));
    sessionService.revokeAll(adminAccountId);
    return sessionService.create(account);
  }

  /** 로그인한 관리자 계정과 새 세션을 함께 반환한다. */
  public record SignInResult(AdminAccount account, AdminSessionService.CreatedSession session) {}
}
