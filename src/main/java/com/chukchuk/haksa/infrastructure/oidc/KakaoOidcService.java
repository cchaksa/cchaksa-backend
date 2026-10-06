package com.chukchuk.haksa.infrastructure.oidc;

import com.chukchuk.haksa.domain.user.service.OidcService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.infrastructure.oidc.kakao.KakaoIdTokenVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Kakao 공개키와 issuer·audience·nonce 조건으로 ID token을 검증한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOidcService implements OidcService {
  private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";
  private static final String KAKAO_CACHE_KEY = "kakao";

  private final OidcJwksClient oidcJwksClient;

  @Value("${security.appKey}")
  private String appKey;

  @Value("${security.nativeAppKey:}")
  private String nativeAppKey;

  /**
   * Kakao ID token의 서명, 만료, issuer, audience와 nonce를 검증한다.
   *
   * <p>token의 {@code kid}가 캐시에 없으면 JWKS를 한 번 새로 조회한다.
   *
   * @param idToken ID token
   * @param expectedNonce 로그인 요청에서 발급한 원본 nonce
   * @return 검증된 Kakao ID token claim
   * @throws TokenException token 형식, 서명, claim 또는 공개키를 검증할 수 없는 경우
   */
  public Claims verifyIdToken(String idToken, String expectedNonce) {
    return new KakaoIdTokenVerifier(oidcJwksClient, new ObjectMapper())
        .verify(idToken, expectedNonce, resolveAllowedAudiences());
  }

  private PublicKey createPublicKey(JsonNode keyNode) throws Exception {
    // RSA 키 파라미터 추출
    BigInteger modulus =
        new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("n").asText()));
    BigInteger exponent =
        new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("e").asText()));

    // RSA 공개키 생성
    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
    KeyFactory factory = KeyFactory.getInstance("RSA");
    return factory.generatePublic(spec);
  }

  private void validateClaims(String expectedNonce, Claims claims) {
    Date expiration = claims.getExpiration();
    if (expiration == null || expiration.before(new Date())) {
      throw new TokenException(ErrorCode.TOKEN_EXPIRED);
    }

    if (!"https://kauth.kakao.com".equals(claims.getIssuer())) {
      throw new TokenException(ErrorCode.TOKEN_INVALID_ISS);
    }

    validateAudience(claims.get("aud"));

    String hashedNonce = hashSha256(expectedNonce);
    String nonce = claims.get("nonce", String.class);

    if (!hashedNonce.equals(nonce)) {
      throw new TokenException(ErrorCode.TOKEN_INVALID_NONCE);
    }
  }

  private JsonNode resolveKeyWithFallback(JsonNode jwks, String kid) {
    JsonNode keyNode = findMatchingKey(jwks, kid);
    if (keyNode != null) {
      return keyNode;
    }

    log.warn("Kakao JWKS key not found in cache, refreshing. kid={}", kid);
    JsonNode refreshed = oidcJwksClient.refreshKeys(KAKAO_CACHE_KEY, KAKAO_JWKS_URL);
    keyNode = findMatchingKey(refreshed, kid);
    if (keyNode == null) {
      throw new TokenException(ErrorCode.TOKEN_NO_MATCHING_KEY);
    }
    return keyNode;
  }

  private JsonNode findMatchingKey(JsonNode jwks, String kid) {
    // 일치하는 공개키가 있다면 반환
    for (JsonNode key : jwks.get("keys")) {
      if (key.get("kid").asText().equals(kid)) {
        return key;
      }
    }
    return null;
  }

  private void validateAudience(Object audClaim) {
    Set<String> allowedAudiences = resolveAllowedAudiences();

    if (audClaim instanceof String aud) {
      if (!allowedAudiences.contains(aud)) {
        throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
      }
      return;
    }

    if (audClaim instanceof List<?> audiences) {
      boolean matched =
          audiences.stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .anyMatch(allowedAudiences::contains);

      if (!matched) {
        throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
      }
      return;
    }

    throw new TokenException(ErrorCode.TOKEN_INVALID_AUD_FORMAT);
  }

  private Set<String> resolveAllowedAudiences() {
    Set<String> allowedAudiences = new LinkedHashSet<>();
    addIfPresent(allowedAudiences, appKey);
    addIfPresent(allowedAudiences, nativeAppKey);
    return allowedAudiences;
  }

  private void addIfPresent(Set<String> targets, String value) {
    if (value == null) {
      return;
    }

    String normalized = value.trim();
    if (!normalized.isEmpty()) {
      targets.add(normalized);
    }
  }

  private String hashSha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : encodedHash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new TokenException(ErrorCode.TOKEN_HASH_ERROR, e);
    }
  }
}
