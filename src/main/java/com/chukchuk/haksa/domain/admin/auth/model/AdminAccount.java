package com.chukchuk.haksa.domain.admin.auth.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 개발진이 직접 등록하는 관리자 로컬 계정이다. */
@Entity
@Table(name = "admin_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount extends BaseEntity {
  @Id private UUID id;

  @Column private String provider;

  @Column(name = "social_id")
  private String socialId;

  @Column(name = "login_id", unique = true)
  private String loginId;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "admin_role", nullable = false)
  private AdminRole adminRole;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdminStatus status;

  @Column(name = "created_by", nullable = false)
  private String createdBy;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  /**
   * 직접 등록할 관리자 계정을 생성한다.
   *
   * @param id 관리자 UUID
   * @param loginId 대소문자를 구분하는 로그인 ID 원문
   * @param passwordHash BCrypt 비밀번호 hash
   * @param displayName 표시 이름
   * @param adminRole 관리자 역할
   * @param status 계정 상태
   * @param createdBy 등록 주체
   */
  public AdminAccount(
      UUID id,
      String loginId,
      String passwordHash,
      String displayName,
      AdminRole adminRole,
      AdminStatus status,
      String createdBy) {
    this.id = id;
    this.loginId = loginId;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.adminRole = adminRole;
    this.status = status;
    this.createdBy = createdBy;
  }

  /**
   * 로그인 가능한 활성 계정인지 확인한다.
   *
   * @return 활성 계정이면 {@code true}
   */
  public boolean isActive() {
    return status == AdminStatus.ACTIVE;
  }

  /**
   * 마지막 로그인 시각을 기록한다.
   *
   * @param now 로그인 성공 시각
   */
  public void recordLogin(Instant now) {
    lastLoginAt = now;
  }

  /**
   * 검증을 마친 새 비밀번호 hash로 교체한다.
   *
   * @param passwordHash 새 BCrypt hash
   */
  public void changePasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /** 계정을 비활성화한다. */
  public void deactivate() {
    status = AdminStatus.INACTIVE;
  }
}
