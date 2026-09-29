package com.chukchuk.haksa.domain.admin.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 서버가 발급하고 한 번만 소모하는 관리자 OIDC nonce다. */
@Entity
@Table(name = "admin_login_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLoginChallenge {
  @Id private UUID id;

  @Column(nullable = false)
  private String nonce;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /**
   * 신규 challenge를 생성한다.
   *
   * @param id challenge UUID
   * @param nonce 서버 발급 nonce
   * @param expiresAt 만료 시각
   * @param createdAt 생성 시각
   */
  public AdminLoginChallenge(UUID id, String nonce, Instant expiresAt, Instant createdAt) {
    this.id = id;
    this.nonce = nonce;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  /**
   * 현재 시각에 소모 가능한지 확인한다.
   *
   * @param now 검사 시각
   * @return 미사용이고 만료 전이면 {@code true}
   */
  public boolean canUse(Instant now) {
    return usedAt == null && expiresAt.isAfter(now);
  }

  /**
   * 로그인 challenge를 사용 완료로 표시한다.
   *
   * @param now 사용 시각
   */
  public void consume(Instant now) {
    usedAt = now;
  }
}
