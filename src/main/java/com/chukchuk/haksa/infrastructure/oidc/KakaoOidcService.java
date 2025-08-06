package com.chukchuk.haksa.infrastructure.oidc;

import com.chukchuk.haksa.domain.user.service.OidcService;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.exception.TokenException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOidcService implements OidcService {
    private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";

    @Value("${security.appKey}")
    private String appKey;

    public Claims verifyIdToken(String idToken, String expectedNonce) {
        try {
            JsonNode jwks = getPublicKey();

            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new TokenException(ErrorCode.TOKEN_INVALID_FORMAT);
            }

            String headerJson = new String(Base64.getDecoder().decode(parts[0]));
            String kid = new ObjectMapper().readTree(headerJson).get("kid").asText();

            JsonNode keyNode = findMatchingKey(jwks, kid);
            if (keyNode == null) {
                throw new TokenException(ErrorCode.TOKEN_NO_MATCHING_KEY);
            }

            PublicKey publicKey = createPublicKey(keyNode);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

            validateClaims(expectedNonce, claims);

            return claims;

        } catch (TokenException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenException(ErrorCode.TOKEN_PARSE_ERROR);
        }
    }

    private JsonNode getPublicKey() {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(KAKAO_JWKS_URL, JsonNode.class);
    }

    private PublicKey createPublicKey(JsonNode keyNode) throws Exception{
        // RSA 키 파라미터 추출
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("n").asText()));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(keyNode.get("e").asText()));

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

        Object audClaim = claims.get("aud");
        if (audClaim instanceof String) {
            if (!appKey.equals(audClaim)) {
                throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
            }
        } else if (audClaim instanceof java.util.List) {
            if (!((java.util.List<?>) audClaim).contains(appKey)) {
                throw new TokenException(ErrorCode.TOKEN_INVALID_AUD);
            }
        } else {
            throw new TokenException(ErrorCode.TOKEN_INVALID_AUD_FORMAT);
        }

        String hashedNonce = hashSHA256(expectedNonce);
        String nonce = claims.get("nonce", String.class);

        if (!hashedNonce.equals(nonce)) {
            throw new TokenException(ErrorCode.TOKEN_INVALID_NONCE);
        }
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

    private String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new TokenException(ErrorCode.TOKEN_HASH_ERROR);
        }
    }

}
