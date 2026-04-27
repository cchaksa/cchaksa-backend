package com.chukchuk.haksa.infrastructure.oidc;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class AppleOidcServiceTests {

    @Test
    void verifyIdToken_withMatchingKeyAndClaims_returnsClaims() throws Exception {
        String kid = "test-kid";
        String alg = "RS256";
        String clientId = "com.example.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        JsonNode jwks = createJwks(kid, alg, (RSAPublicKey) keyPair.getPublic());

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

        AppleOidcService service = createService(jwksClient, clientId, null, issuer, keysUrl);

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getSubject()).isEqualTo("apple-sub");
        assertThat(claims.get("email", String.class)).isEqualTo("apple@example.com");
    }

    @Test
    void verifyIdToken_refreshesKeysWhenKidMissing() throws Exception {
        String kid = "rotated-kid";
        String alg = "RS256";
        String clientId = "com.example.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        JsonNode staleJwks = createJwks("stale", alg, publicKey);
        JsonNode refreshedJwks = createJwks(kid, alg, publicKey);

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);
        Date expiration = new Date(System.currentTimeMillis() + 60000);

        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setAudience(clientId)
                .setSubject("apple-sub")
                .claim("email", "user@example.com")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys("apple", keysUrl)).thenReturn(staleJwks);
        when(jwksClient.refreshKeys("apple", keysUrl)).thenReturn(refreshedJwks);

        AppleOidcService service = createService(jwksClient, clientId, null, issuer, keysUrl);

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getSubject()).isEqualTo("apple-sub");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        Mockito.verify(jwksClient).refreshKeys("apple", keysUrl);
    }

    @Test
    void verifyIdToken_withAllowedAudienceList_acceptsAlternateClientId() throws Exception {
        String kid = "allowed-kid";
        String alg = "RS256";
        String primaryClientId = "com.cchaksa.app";
        String alternateClientId = "com.chukchukhaksa.moblie.ChukChukHaksa";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        JsonNode jwks = createJwks(kid, alg, (RSAPublicKey) keyPair.getPublic());

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);
        Date expiration = new Date(System.currentTimeMillis() + 60000);

        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setAudience(alternateClientId)
                .setSubject("apple-sub")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys(Mockito.anyString(), Mockito.eq(keysUrl))).thenReturn(jwks);

        AppleOidcService service = createService(
                jwksClient,
                primaryClientId,
                primaryClientId + "," + alternateClientId,
                issuer,
                keysUrl
        );

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getAudience()).isEqualTo(alternateClientId);
    }

    @Test
    void verifyIdToken_withAudClaimArray_acceptsAllowedEntry() throws Exception {
        String kid = "array-kid";
        String alg = "RS256";
        String primaryClientId = "com.cchaksa.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        JsonNode jwks = createJwks(kid, alg, (RSAPublicKey) keyPair.getPublic());

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);
        Date expiration = new Date(System.currentTimeMillis() + 60000);

        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setSubject("apple-sub")
                .claim("aud", List.of("unrelated", primaryClientId))
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys(Mockito.anyString(), Mockito.eq(keysUrl))).thenReturn(jwks);

        AppleOidcService service = createService(jwksClient, primaryClientId, null, issuer, keysUrl);

        Claims claims = service.verifyIdToken(idToken, rawNonce);

        assertThat(claims.getSubject()).isEqualTo("apple-sub");
    }

    @Test
    void verifyIdToken_withAudOutsideAllowList_throwsTokenException() throws Exception {
        String kid = "invalid-aud-kid";
        String alg = "RS256";
        String primaryClientId = "com.cchaksa.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        JsonNode jwks = createJwks(kid, alg, (RSAPublicKey) keyPair.getPublic());

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);
        Date expiration = new Date(System.currentTimeMillis() + 60000);

        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setAudience("com.unknown.app")
                .setSubject("apple-sub")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys(Mockito.anyString(), Mockito.eq(keysUrl))).thenReturn(jwks);

        AppleOidcService service = createService(
                jwksClient,
                primaryClientId,
                primaryClientId + ",com.chukchukhaksa.moblie.ChukChukHaksa",
                issuer,
                keysUrl
        );

        assertThatThrownBy(() -> service.verifyIdToken(idToken, rawNonce))
                .isInstanceOf(TokenException.class)
                .matches(ex -> ((TokenException) ex).getCode().equals(ErrorCode.TOKEN_INVALID_AUD.code()));
    }

    @Test
    void verifyIdToken_withNonceMismatch_propagatesOriginalTokenException() throws Exception {
        String kid = "nonce-kid";
        String alg = "RS256";
        String clientId = "com.example.app";
        String issuer = "https://appleid.apple.com";
        String keysUrl = "https://appleid.apple.com/auth/keys";

        KeyPair keyPair = generateRsaKeyPair();
        JsonNode jwks = createJwks(kid, alg, (RSAPublicKey) keyPair.getPublic());

        String rawNonce = "nonce-value";
        String hashedNonce = hashSHA256(rawNonce);
        Date expiration = new Date(System.currentTimeMillis() + 60000);

        String idToken = Jwts.builder()
                .setHeaderParam("kid", kid)
                .setHeaderParam("alg", alg)
                .setIssuer(issuer)
                .setAudience(clientId)
                .setSubject("apple-sub")
                .claim("nonce", hashedNonce)
                .setExpiration(expiration)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        OidcJwksClient jwksClient = Mockito.mock(OidcJwksClient.class);
        when(jwksClient.fetchKeys(Mockito.anyString(), Mockito.eq(keysUrl))).thenReturn(jwks);

        AppleOidcService service = createService(jwksClient, clientId, null, issuer, keysUrl);

        assertThatThrownBy(() -> service.verifyIdToken(idToken, "different-nonce"))
                .isInstanceOf(TokenException.class)
                .matches(ex -> ((TokenException) ex).getCode().equals(ErrorCode.TOKEN_INVALID_NONCE.code()));
    }

    private AppleOidcService createService(OidcJwksClient jwksClient,
                                           String clientId,
                                           String allowedClientIds,
                                           String issuer,
                                           String keysUrl) {

        AppleOidcService service = new AppleOidcService(jwksClient);
        ReflectionTestUtils.setField(service, "clientId", clientId);
        ReflectionTestUtils.setField(service, "allowedClientIds", allowedClientIds);
        ReflectionTestUtils.setField(service, "issuer", issuer);
        ReflectionTestUtils.setField(service, "keysUrl", keysUrl);
        return service;
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private JsonNode createJwks(String kid, String alg, RSAPublicKey publicKey) throws Exception {
        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(publicKey.getPublicExponent().toByteArray());

        ObjectMapper mapper = new ObjectMapper();
        String jwksJson = "{\"keys\":[{\"kid\":\"" + kid + "\",\"alg\":\"" + alg + "\",\"kty\":\"RSA\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";
        return mapper.readTree(jwksJson);
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
