package com.chukchuk.haksa.infrastructure.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class KakaoOidcServiceTests {

    @Test
    void verifyIdToken_refreshesKeysWhenKidMissing() throws Exception {
        String kid = "new-kid";
        String appKey = "kakao-app-key";

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(stripLeadingZero(publicKey.getModulus().toByteArray()));
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(stripLeadingZero(publicKey.getPublicExponent().toByteArray()));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode staleJwks = mapper.readTree("{\"keys\":[{\"kid\":\"old\",\"kty\":\"RSA\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}");
        JsonNode refreshedJwks = mapper.readTree("{\"keys\":[{\"kid\":\"" + kid + "\",\"kty\":\"RSA\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}");

        String rawNonce = "nonce";
        String hashedNonce = hashSHA256(rawNonce);

        Date expiration = new Date(System.currentTimeMillis() + 60000);
        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", "RS256")
                .setIssuer("https://kauth.kakao.com")
                .setAudience(appKey)
                .setSubject("kakao-sub")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys("kakao", "https://kauth.kakao.com/.well-known/jwks.json")).thenReturn(staleJwks);
        when(jwksClient.refreshKeys("kakao", "https://kauth.kakao.com/.well-known/jwks.json")).thenReturn(refreshedJwks);

        KakaoOidcService service = new KakaoOidcService(jwksClient);
        ReflectionTestUtils.setField(service, "appKey", appKey);

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getSubject()).isEqualTo("kakao-sub");
        Mockito.verify(jwksClient).refreshKeys("kakao", "https://kauth.kakao.com/.well-known/jwks.json");
    }

    private static byte[] stripLeadingZero(byte[] value) {
        if (value.length > 1 && value[0] == 0) {
            byte[] copy = new byte[value.length - 1];
            System.arraycopy(value, 1, copy, 0, copy.length);
            return copy;
        }
        return value;
    }

    private String hashSHA256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
