package com.chukchuk.haksa.global.security;

import com.chukchuk.haksa.domain.user.model.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String profileNickname;
    private final String profileImage;
    private final boolean isDeleted;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.profileNickname = user.getProfileNickname();
        this.profileImage = user.getProfileImage();
        this.isDeleted = user.getIsDeleted();
    }

    public UUID getId() {
        return id;
    }

    public String getProfileNickname() {
        return profileNickname;
    }

    public String getProfileImage() {
        return profileImage;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // 현재는 권한이 필요 없으므로 빈 리스트 반환
    }

    @Override
    public String getPassword() {
        return null; // Supabase 인증에서는 패스워드를 사용하지 않음
    }

    @Override
    public String getUsername() {
        return email;
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