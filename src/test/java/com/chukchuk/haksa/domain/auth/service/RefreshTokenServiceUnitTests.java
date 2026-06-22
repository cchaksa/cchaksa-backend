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

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("save는 refresh token 엔티티를 생성해 저장한다")
    void save_persistsRefreshToken() {
        Date expiry = new Date(System.currentTimeMillis() + 60_000);
        when(refreshTokenHasher.hash("refresh-token")).thenReturn("refresh-token-hash");

        refreshTokenService.save("session-1", "user-1", "refresh-token", expiry);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo("session-1");
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getTokenHash()).isEqualTo("refresh-token-hash");
    }

    @Test
    @DisplayName("reissue 성공 시 access/refresh 토큰을 재발급한다")
    void reissue_success() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String sessionId = "session-1";
        String oldRefresh = "old-refresh";
        Date newExpiry = new Date(System.currentTimeMillis() + 120_000);

        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();

        when(jwtProvider.parseToken(oldRefresh)).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, null, "old-refresh-hash", new Date())));
        when(refreshTokenHasher.matches(oldRefresh, "old-refresh-hash")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdText, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(userIdText, sessionId))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("new-refresh", newExpiry, sessionId));
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("new-refresh-hash");

        AuthDto.RefreshResponse response = refreshTokenService.reissue(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getUserId()).isEqualTo(userIdText);
        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getTokenHash()).isEqualTo("new-refresh-hash");
    }

    @Test
    @DisplayName("sid가 없는 기존 refresh token은 userId 기반 세션 row로 재발급한다")
    void reissue_legacyTokenWithoutSessionId_usesUserIdFallback() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String oldRefresh = "legacy-refresh";
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
                .thenReturn(Optional.of(new RefreshToken(userIdText, userIdText, oldRefresh, new Date())));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdText, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(userIdText, userIdText))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("new-refresh", newExpiry, userIdText));
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("new-refresh-hash");

        AuthDto.RefreshResponse response = refreshTokenService.reissue(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getTokenHash()).isEqualTo("new-refresh-hash");
    }

    @Test
    @DisplayName("hash로 저장된 refresh token과 요청 token이 다르면 REFRESH_TOKEN_MISMATCH 예외를 던진다")
    void reissue_tokenHashMismatch_throws() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String sessionId = "session-1";
        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);
        when(jwtProvider.parseToken("request-token")).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, null, "stored-hash", new Date())));
        when(refreshTokenHasher.matches("request-token", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> refreshTokenService.reissue("request-token"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_MISMATCH.message());
    }

    @Test
    @DisplayName("reissue는 refresh token 만료가 임계값보다 많이 남으면 기존 refresh token을 반환한다")
    void reissue_refreshExpiryBeyondThreshold_returnsExistingRefreshToken() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String sessionId = "session-1";
        String oldRefresh = "old-refresh";
        Date savedExpiry = new Date(System.currentTimeMillis() + Duration.ofDays(8).toMillis());

        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();

        when(jwtProvider.parseToken(oldRefresh)).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, oldRefresh, savedExpiry)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdText, "user@example.com", "USER")).thenReturn("new-access");
        when(refreshTokenHasher.hash(oldRefresh)).thenReturn("old-refresh-hash");

        AuthDto.RefreshResponse response = refreshTokenService.reissue(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo(oldRefresh);
        verify(jwtProvider, never()).createRefreshToken(userIdText, sessionId);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getTokenHash()).isEqualTo("old-refresh-hash");
    }

    @Test
    @DisplayName("reissue는 저장된 refresh token 만료시각이 없으면 새 refresh token을 저장한다")
    void reissue_refreshExpiryNull_renewsRefreshToken() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String sessionId = "session-1";
        String oldRefresh = "old-refresh";
        Date newExpiry = new Date(System.currentTimeMillis() + Duration.ofDays(14).toMillis());

        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);

        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .profileNickname("user")
                .build();

        when(jwtProvider.parseToken(oldRefresh)).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, oldRefresh, null)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtProvider.createAccessToken(userIdText, "user@example.com", "USER")).thenReturn("new-access");
        when(jwtProvider.createRefreshToken(userIdText, sessionId))
                .thenReturn(new AuthDto.RefreshTokenWithExpiry("new-refresh", newExpiry, sessionId));
        when(refreshTokenHasher.hash("new-refresh")).thenReturn("new-refresh-hash");

        AuthDto.RefreshResponse response = refreshTokenService.reissue(oldRefresh);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getSessionId()).isEqualTo(sessionId);
        assertThat(captor.getValue().getToken()).isNull();
        assertThat(captor.getValue().getTokenHash()).isEqualTo("new-refresh-hash");
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
        String sessionId = "session-1";
        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);
        when(jwtProvider.parseToken("request-token")).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, "different-token", new Date())));

        assertThatThrownBy(() -> refreshTokenService.reissue("request-token"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_MISMATCH.message());
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void reissue_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        String userIdText = userId.toString();
        String sessionId = "session-1";
        Claims claims = Jwts.claims();
        claims.setSubject(userIdText);
        claims.put("sid", sessionId);
        when(jwtProvider.parseToken("refresh-token")).thenReturn(claims);
        when(refreshTokenRepository.findById(sessionId))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userIdText, "refresh-token", new Date())));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.reissue("refresh-token"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.message());
    }

    @Test
    @DisplayName("findBySessionId는 저장된 refresh token을 반환한다")
    void findBySessionId_success() {
        RefreshToken token = new RefreshToken("session-1", "user-1", "refresh", new Date());
        when(refreshTokenRepository.findById("session-1")).thenReturn(Optional.of(token));

        RefreshToken found = refreshTokenService.findBySessionId("session-1");

        assertThat(found).isSameAs(token);
    }

    @Test
    @DisplayName("findBySessionId 대상이 없으면 REFRESH_TOKEN_NOT_FOUND 예외를 던진다")
    void findBySessionId_notFound_throws() {
        when(refreshTokenRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findBySessionId("missing"))
                .isInstanceOf(TokenException.class)
                .hasMessage(ErrorCode.REFRESH_TOKEN_NOT_FOUND.message());
    }

    @Test
    @DisplayName("만료 토큰 정리 작업은 삭제 쿼리를 호출한다")
    void deletedExpiredTokens_executesDelete() {
        when(refreshTokenRepository.deleteByExpiryBefore(any(Date.class))).thenReturn(3);

        int deleted = refreshTokenService.deletedExpiredTokens();

        assertThat(deleted).isEqualTo(3);
        verify(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));
    }

    @Test
    @DisplayName("만료 토큰 정리 중 예외가 발생하면 호출자에게 전파한다")
    void deletedExpiredTokens_propagatesException() {
        doThrow(new RuntimeException("db error"))
                .when(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));

        assertThatThrownBy(() -> refreshTokenService.deletedExpiredTokens())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
        verify(refreshTokenRepository).deleteByExpiryBefore(any(Date.class));
    }
}
