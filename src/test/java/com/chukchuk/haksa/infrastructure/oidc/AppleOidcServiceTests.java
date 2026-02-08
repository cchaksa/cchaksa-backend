package com.chukchuk.haksa.infrastructure.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AppleOidcServiceTests {

    @Test
    void verifyIdToken_withMatchingKeyAndClaims_returnsClaims() throws Exception {
        String kid = "test-kid";
        String alg = "RS256";
        String clientId = "com.example.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getPublicExponent().toByteArray());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jwks = mapper.readTree("{\"keys\":[{" +
                "\"kid\":\"" + kid + "\"," +
                "\"alg\":\"" + alg + "\"," +
                "\"kty\":\"RSA\"," +
                "\"n\":\"" + n + "\"," +
                "\"e\":\"" + e + "\"" +
                "}]}");

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);

        Date expiration = new Date(System.currentTimeMillis() + 60000);
        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setAudience(clientId)
                .setSubject("apple-sub")
                .claim("email", "apple@example.com")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys(Mockito.anyString(), Mockito.eq(keysUrl))).thenReturn(jwks);

        AppleOidcService service = new AppleOidcService(jwksClient);
        ReflectionTestUtils.setField(service, "clientId", clientId);
        ReflectionTestUtils.setField(service, "issuer", issuer);
        ReflectionTestUtils.setField(service, "keysUrl", keysUrl);

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getSubject()).isEqualTo("apple-sub");
        assertThat(claims.get("email", String.class)).isEqualTo("apple@example.com");
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
