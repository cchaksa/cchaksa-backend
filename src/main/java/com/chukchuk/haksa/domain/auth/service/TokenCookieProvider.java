package com.chukchuk.haksa.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class TokenCookieProvider {

    private static final String ACCESS_COOKIE = "accessToken";
    private static final String REFRESH_COOKIE = "refreshToken";

    @Value("${security.jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${security.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Value("${security.cookie.dev-mode:false}")
    private boolean devMode;

    public ResponseCookie createAccessTokenCookie(String accessToken) {
        return baseCookieBuilder(ACCESS_COOKIE, accessToken, accessTokenExpiration).build();
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return baseCookieBuilder(REFRESH_COOKIE, refreshToken, refreshTokenExpiration).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookieBuilder(String name, String value, long expirationMillis) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(!devMode)
                .path("/")
                .sameSite(devMode ? "Lax" : "None")
                .maxAge(Duration.ofMillis(expirationMillis));
    }
}
