// JWT 토큰 발급과 파싱 동작을 검증하는 테스트
package com.chukchuk.haksa.global.security.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTests {

    @Test
    @DisplayName("이메일이 없으면 access token에서 email claim을 생략한다")
    void createAccessToken_withoutEmail_omitsEmailClaim() {
        JwtProvider jwtProvider = jwtProvider();

        Claims claims = jwtProvider.parseToken(jwtProvider.createAccessToken("user-1", null, "USER"));

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims).doesNotContainKey("email");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("refresh token에는 로그인 세션 식별자 sid가 포함된다")
    void createRefreshToken_containsSessionIdClaim() {
        JwtProvider jwtProvider = jwtProvider();

        AuthDto.RefreshTokenWithExpiry refresh = jwtProvider.createRefreshToken("user-1");

        Claims claims = jwtProvider.parseToken(refresh.token());
        assertThat(refresh.sessionId()).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("sid", String.class)).isEqualTo(refresh.sessionId());
    }

    @Test
    @DisplayName("같은 사용자에게 refresh token을 여러 번 발급해도 sid는 매번 달라진다")
    void createRefreshToken_generatesDifferentSessionIdEachTime() {
        JwtProvider jwtProvider = jwtProvider();

        AuthDto.RefreshTokenWithExpiry first = jwtProvider.createRefreshToken("user-1");
        AuthDto.RefreshTokenWithExpiry second = jwtProvider.createRefreshToken("user-1");

        assertThat(first.sessionId()).isNotEqualTo(second.sessionId());
    }

    @Test
    @DisplayName("refresh token 재발급 시 기존 sid를 유지할 수 있다")
    void createRefreshToken_withSessionId_preservesSessionIdClaim() {
        JwtProvider jwtProvider = jwtProvider();

        AuthDto.RefreshTokenWithExpiry refresh = jwtProvider.createRefreshToken("user-1", "session-1");

        Claims claims = jwtProvider.parseToken(refresh.token());
        assertThat(refresh.sessionId()).isEqualTo("session-1");
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("sid", String.class)).isEqualTo("session-1");
    }

    private JwtProvider jwtProvider() {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiration", 3_600_000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiration", 604_800_000L);
        jwtProvider.init();
        return jwtProvider;
    }
}
