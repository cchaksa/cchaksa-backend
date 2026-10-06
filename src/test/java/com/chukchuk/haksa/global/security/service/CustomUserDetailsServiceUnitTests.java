package com.chukchuk.haksa.global.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceUnitTests {

  @Mock private UserRepository userRepository;

  @InjectMocks private CustomUserDetailsService customUserDetailsService;

  @Test
  @DisplayName("사용자가 존재하면 CustomUserDetails를 반환한다")
  void loadUserByUsernameSuccess() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder().id(userId).email("test@example.com").profileNickname("tester").build();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UserDetails userDetails = customUserDetailsService.loadUserByUsername(userId.toString());

    assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
    assertThat(userDetails.getUsername()).isEqualTo(userId.toString());
  }

  @Test
  @DisplayName("사용자가 없으면 USER_NOT_FOUND 토큰 예외를 던진다")
  void loadUserByUsernameNotFoundThrows() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(userId.toString()))
        .isInstanceOf(TokenException.class)
        .hasMessage("해당 사용자를 찾을 수 없습니다.");
  }
}
