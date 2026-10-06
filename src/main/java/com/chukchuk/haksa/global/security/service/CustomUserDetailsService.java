package com.chukchuk.haksa.global.security.service;

import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** JWT subject의 사용자 UUID로 인증 계정 정보를 조회한다. */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {

    return userRepository
        .findById(UUID.fromString(userId))
        .map(CustomUserDetails::new)
        .orElseThrow(() -> new TokenException(ErrorCode.USER_NOT_FOUND));
  }
}
