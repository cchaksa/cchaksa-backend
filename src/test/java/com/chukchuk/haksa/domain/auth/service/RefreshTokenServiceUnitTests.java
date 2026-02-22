package com.chukchuk.haksa.domain.auth.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.entity.RefreshToken;
import com.chukchuk.haksa.domain.auth.repository.RefreshTokenRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceUnitTests {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("save는 refresh token 엔티티를 생성해 저장한다")
    void save_persistsRefreshToken() {
        Date expiry = new Date(System.currentTimeMillis() + 60_000);

        refreshTokenService.save("user-1", "refresh-token", expiry);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("reissue 성공 시 access/refresh 토큰을 재발급한다")
    void reissue_success() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String oldRefresh = "old-refresh";
        Date newExpiry = new Date(System.currentTimeMillis() + 120_000);

        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();

        when(jwtProvider.parseToken(oldRefresh)).thenReturn(claims);
        when(refreshTokenRepository.findById(userIdText))
                .thenReturn(Optional.of(new RefreshToken(userIdText, oldRefresh, new Date())));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdText, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(userIdText))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("new-refresh", newExpiry));

        AuthDto.RefreshResponse response = refreshTokenService.reissue(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("저장된 refresh token이 없으면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void reissue_refreshNotFound_throws() {
        UUID userId = UUID.randomUUID();
        Claims claims = Jwts.claims();
        claims.setSubject(userId.toString());
        when(jwtProvider.parseToken("missing")).thenReturn(claims);
        when(refreshTokenRepository.findById(userId.toString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.reissue("missing"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_FOUND.message());
    }

    @Test
    @DisplayName("저장된 refresh token과 요청 token이 다르면 REFRESH_TOKEN_MISMATCH 예외를 던진다")
    void reissue_tokenMismatch_throws() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        when(jwtProvider.parseToken("request-token")).thenReturn(claims);
        when(refreshTokenRepository.findById(userIdText))
                .thenReturn(Optional.of(new RefreshToken(userIdText, "different-token", new Date())));

        assertThatThrownBy(() -> refreshTokenService.reissue("request-token"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_MISMATCH.message());
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void reissue_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        when(jwtProvider.parseToken("refresh-token")).thenReturn(claims);
        when(refreshTokenRepository.findById(userIdText))
                .thenReturn(Optional.of(new RefreshToken(userIdText, "refresh-token", new Date())));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.reissue("refresh-token"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.message());
    }

    @Test
    @DisplayName("findByUserId는 저장된 refresh token을 반환한다")
    void findByUserId_success() {
        RefreshToken token = new RefreshToken("user-1", "refresh", new Date());
        when(refreshTokenRepository.findById("user-1")).thenReturn(Optional.of(token));

        RefreshToken found = refreshTokenService.findByUserId("user-1");

        assertThat(found).isSameAs(token);
    }

    @Test
    @DisplayName("findByUserId 대상이 없으면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void findByUserId_notFound_throws() {
        when(refreshTokenRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findByUserId("missing"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_FOUND.message());
    }

    @Test
    @DisplayName("만료 토큰 정리 작업은 삭제 쿼리를 호출한다")
    void deletedExpiredTokens_executesDelete() {
        when(refreshTokenRepository.deleteByExpiryBefore(any(Date.class))).thenReturn(3);

        refreshTokenService.deletedExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));
    }

    @Test
    @DisplayName("만료 토큰 정리 중 예외가 발생해도 예외를 전파하지 않는다")
    void deletedExpiredTokens_swallowsException() {
        doThrow(new RuntimeException("db error"))
                .when(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));

        refreshTokenService.deletedExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));
    }
}
