package com.chukchuk.haksa.infrastructure.oidc;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class KakaoOidcServiceAudValidationTests {

    private static final String APP_KEY = "rest-app-key";
    private static final String NATIVE_APP_KEY = "native-app-key";
    private static final String RAW_NONCE = "nonce-raw-value";

    private KakaoOidcService kakaoOidcService;
    private Method validateClaimsMethod;

    @BeforeEach
    void setUp() throws Exception {
        kakaoOidcService = new KakaoOidcService(mock(OidcJwksClient.class));
        validateClaimsMethod = KakaoOidcService.class.getDeclaredMethod("validateClaims", String.class, Claims.class);
        validateClaimsMethod.setAccessible(true);

        setPrivateField("appKey", APP_KEY);
        setPrivateField("nativeAppKey", NATIVE_APP_KEY);
    }

    @Test
    @DisplayName("aud가 APP_KEY와 일치하면 검증에 성공한다")
    void validateClaimsPassesWhenAudMatchesAppKey() {
        Claims claims = validClaims(APP_KEY);

        invokeValidateClaims(RAW_NONCE, claims);
    }

    @Test
    @DisplayName("aud가 APP_NATIVE_KEY와 일치하면 검증에 성공한다")
    void validateClaimsPassesWhenAudMatchesNativeAppKey() {
        Claims claims = validClaims(NATIVE_APP_KEY);

        invokeValidateClaims(RAW_NONCE, claims);
    }

    @Test
    @DisplayName("aud가 배열일 때 허용 키 중 하나라도 포함하면 검증에 성공한다")
    void validateClaimsPassesWhenAudListContainsAllowedAudience() {
        Claims claims = validClaims(List.of("unknown-aud", NATIVE_APP_KEY));

        invokeValidateClaims(RAW_NONCE, claims);
    }

    @Test
    @DisplayName("aud가 허용 키와 모두 불일치하면 T06 예외를 던진다")
    void validateClaimsThrowsWhenAudienceDoesNotMatch() {
        Claims claims = validClaims("unknown-aud");

        assertThatThrownBy(() -> invokeValidateClaims(RAW_NONCE, claims))
                .isInstanceOf(TokenException.class)
                .extracting(ex -> ((TokenException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID_AUD);
    }

    @Test
    @DisplayName("aud 형식이 문자열/배열이 아니면 T07 예외를 던진다")
    void validateClaimsThrowsWhenAudienceFormatIsInvalid() {
        Claims claims = validClaims(12345);

        assertThatThrownBy(() -> invokeValidateClaims(RAW_NONCE, claims))
                .isInstanceOf(TokenException.class)
                .extracting(ex -> ((TokenException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID_AUD_FORMAT);
    }

    private Claims validClaims(Object audClaim) {
        DefaultClaims claims = new DefaultClaims();
        claims.setExpiration(new Date(System.currentTimeMillis() + 60_000));
        claims.setIssuer("https://kauth.kakao.com");
        claims.put("aud", audClaim);
        claims.put("nonce", hashSha256(RAW_NONCE));
        return claims;
    }

    private void invokeValidateClaims(String expectedNonce, Claims claims) {
        try {
            validateClaimsMethod.invoke(kakaoOidcService, expectedNonce, claims);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        }
    }

    private void setPrivateField(String fieldName, String value) throws Exception {
        Field field = KakaoOidcService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(kakaoOidcService, value);
    }

    private String hashSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
