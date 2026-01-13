package com.chukchuk.haksa.domain.auth.service;

import com.chukchuk.haksa.global.security.AuthCookieNames;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenCookieProvider {

    @Value("${security.jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Value("${security.cookie.dev-mode:false}")
    private boolean devMode;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return buildCookie(AuthCookieNames.ACCESS, accessToken, accessTokenExpiration);
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return buildCookie(AuthCookieNames.REFRESH, refreshToken, refreshTokenExpiration);
    }

    public ResponseCookie expireAccessTokenCookie() {
        return expireCookie(AuthCookieNames.ACCESS);
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return expireCookie(AuthCookieNames.REFRESH);
    }

    private ResponseCookie buildCookie(String name, String value, long expirationMillis) {
        return baseBuilder(name, value, Duration.ofMillis(expirationMillis)).build();
    }

    private ResponseCookie expireCookie(String name) {
        return baseBuilder(name, "", Duration.ZERO).maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(!devMode)
                .path("/")
                .sameSite(devMode ? "Lax" : "None")
                .maxAge(maxAge);
    }
}
