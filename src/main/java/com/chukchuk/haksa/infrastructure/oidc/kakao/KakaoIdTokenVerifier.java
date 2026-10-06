package com.chukchuk.haksa.infrastructure.oidc.kakao;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.infrastructure.oidc.OidcJwksClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 앱별 설정과 분리해 카카오 ID Token의 암호학적 검증을 수행한다. */
@Component
@RequiredArgsConstructor
public class KakaoIdTokenVerifier {
  private static final String ISSUER = "https://kauth.kakao.com";
  private static final String JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";
  private static final String CACHE_KEY = "kakao";

  private final OidcJwksClient jwksClient;
  private final ObjectMapper objectMapper;

  /**
   * 카카오 토큰의 서명과 표준 claim을 검증한다.
   *
   * @param idToken 검증할 ID Token
   * @param expectedNonce 서버가 발급한 nonce
   * @param allowedAudiences 허용할 앱 key 집합
   * @return 검증된 claim
   * @throws TokenException 토큰 서명이나 claim이 유효하지 않은 경우
   */
  public Claims verify(String idToken, String expectedNonce, Set<String> allowedAudiences) {
    try {
      String[] parts = idToken.split("\\.");
      if (parts.length != 3) {
        throw new TokenException(ErrorCode.TOKEN_INVALID_FORMAT);
      }
      String headerJson =
          new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
      JsonNode kidNode = objectMapper.readTree(headerJson).get("kid");
      if (kidNode == null || kidNode.asText().isBlank()) {
        throw new TokenException(ErrorCode.TOKEN_INVALID_FORMAT);
      }

      JsonNode key = resolveKey(kidNode.asText());
      Claims claims =
          Jwts.parserBuilder()
              .setSigningKey(createPublicKey(key))
              .build()
              .parseClaimsJws(idToken)
              .getBody();
      validateClaims(claims, expectedNonce, allowedAudiences);
      return claims;
    } catch (TokenException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new TokenException(ErrorCode.TOKEN_PARSE_ERROR, exception);
    }
  }

  private JsonNode resolveKey(String kid) {
    JsonNode key = findKey(jwksClient.fetchKeys(CACHE_KEY, JWKS_URL), kid);
    if (key == null) {
      key = findKey(jwksClient.refreshKeys(CACHE_KEY, JWKS_URL), kid);
    }
    if (key == null) {
      throw new TokenException(ErrorCode.TOKEN_NO_MATCHING_KEY);
    }
    return key;
  }

  private JsonNode findKey(JsonNode jwks, String kid) {
    JsonNode keys = jwks.get("keys");
    if (keys == null || !keys.isArray()) {
      return null;
    }
    for (JsonNode key : keys) {
      if (kid.equals(key.path("kid").asText())) {
        return key;
      }
    }
    return null;
  }

  private PublicKey createPublicKey(JsonNode key) throws Exception {
    BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("n").asText()));
    BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.get("e").asText()));
    return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
  }

  private void validateClaims(Claims claims, String expectedNonce, Set<String> allowedAudiences)
      throws Exception {
    Date expiration = claims.getExpiration();
    if (expiration == null || expiration.before(new Date())) {
      throw new TokenException(ErrorCode.TOKEN_EXPIRED);
    }
    if (!ISSUER.equals(claims.getIssuer())) {
      throw new TokenException(ErrorCode.TOKEN_INVALID_ISS);
    }
    validateAudience(claims.get("aud"), allowedAudiences);
    String actualNonce = claims.get("nonce", String.class);
    if (!sha256(expectedNonce).equals(actualNonce)) {
      throw new TokenException(ErrorCode.TOKEN_INVALID_NONCE);
    }
  }

  private void validateAudience(Object claim, Set<String> allowedAudiences) {
    if (claim instanceof String audience) {
      if (!allowedAudiences.contains(audience)) {
        throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
      }
      return;
    }
    if (claim instanceof List<?> audiences
        && audiences.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .anyMatch(allowedAudiences::contains)) {
      return;
    }
    if (claim instanceof List<?>) {
      throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
    }
    throw new TokenException(ErrorCode.TOKEN_INVALID_AUD_FORMAT);
  }

  private String sha256(String value) throws Exception {
    return java.util.HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
