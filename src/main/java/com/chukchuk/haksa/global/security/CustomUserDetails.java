package com.chukchuk.haksa.global.security;

import com.chukchuk.haksa.domain.user.model.User;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 인증된 사용자의 식별 정보와 권한을 제공한다. */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  private final UUID id;
  private final String email;
  private final String profileNickname;
  private final String profileImage;
  private final boolean isDeleted;

  /**
   * 사용자 엔티티의 인증 식별자와 계정 활성 상태를 Spring Security 형식으로 변환한다.
   *
   * @param user 인증 문맥에 반영할 사용자 엔티티
   */
  public CustomUserDetails(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.profileNickname = user.getProfileNickname();
    this.profileImage = user.getProfileImage();
    this.isDeleted = user.getIsDeleted();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList(); // 현재는 권한이 필요 없으므로 빈 리스트 반환
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public String getUsername() {
    return id.toString();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !isDeleted;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return !isDeleted;
  }
}
