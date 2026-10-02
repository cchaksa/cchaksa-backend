package com.chukchuk.haksa.domain.admin.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 폐기 가능한 관리자 쿠키 세션의 해시와 수명을 저장한다. */
@Entity
@Table(name = "admin_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminSession {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "admin_account_id", nullable = false)
  private AdminAccount adminAccount;

  @Column(name = "token_hash", nullable = false)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "idle_expires_at", nullable = false)
  private Instant idleExpiresAt;

  @Column(name = "last_accessed_at", nullable = false)
  private Instant lastAccessedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * 신규 관리자 세션을 생성한다.
   *
   * @param id 세션 UUID
   * @param adminAccount 관리자 계정
   * @param tokenHash 세션 원문 해시
   * @param expiresAt 절대 만료 시각
   * @param idleExpiresAt 유휴 만료 시각
   * @param now 생성 시각
   */
  public AdminSession(
      UUID id,
      AdminAccount adminAccount,
      String tokenHash,
      Instant expiresAt,
      Instant idleExpiresAt,
      Instant now) {
    this.id = id;
    this.adminAccount = adminAccount;
    this.tokenHash = tokenHash;
    this.expiresAt = expiresAt;
    this.idleExpiresAt = idleExpiresAt;
    this.lastAccessedAt = now;
    this.createdAt = now;
  }

  /**
   * 폐기 여부와 두 만료 시각을 함께 검사한다.
   *
   * @param now 검사 시각
   * @return 사용 가능하면 {@code true}
   */
  public boolean isUsable(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now) && idleExpiresAt.isAfter(now);
  }

  /**
   * 마지막 접근과 idle 만료 시각을 갱신한다.
   *
   * @param now 접근 시각
   * @param idleTimeout 유휴 제한 시간
   */
  public void touch(Instant now, Duration idleTimeout) {
    lastAccessedAt = now;
    Instant nextIdleExpiry = now.plus(idleTimeout);
    idleExpiresAt = nextIdleExpiry.isBefore(expiresAt) ? nextIdleExpiry : expiresAt;
  }

  /**
   * 세션을 폐기한다.
   *
   * @param now 폐기 시각
   */
  public void revoke(Instant now) {
    if (revokedAt == null) {
      revokedAt = now;
    }
  }
}
