package com.chukchuk.haksa.domain.user.model;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** OIDC 공급자의 사용자 식별자를 척척학사 사용자 계정과 연결한다. */
@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_provider_social_id",
          columnNames = {"provider", "social_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "provider", nullable = false)
  @Enumerated(EnumType.STRING)
  private OidcProvider provider;

  @Column(name = "social_id", nullable = false, length = 255)
  private String socialId;

  @Column(name = "email")
  private String email;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * 소셜 제공자 계정과 사용자의 연결을 생성한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param socialId social id 식별자
   * @param email 연락 및 로그인에 사용하는 이메일
   * @param user 연결할 사용자
   */
  @Builder
  public SocialAccount(OidcProvider provider, String socialId, String email, User user) {
    this.provider = provider;
    this.socialId = socialId;
    this.email = email;
    this.user = user;
  }

  /**
   * 연결된 사용자 참조를 새 사용자로 교체한다.
   *
   * @param user 대상 사용자
   */
  public void updateUser(User user) {
    this.user = user;
  }
}
