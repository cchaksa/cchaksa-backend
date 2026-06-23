// 포털 로그인 검증 token을 stateless 방식으로 발급하고 검증하는 서비스
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PortalLoginVerificationTokenService {

    private static final String PORTAL_TYPE_CLAIM = "portal_type";
    private static final String USERNAME_HASH_CLAIM = "username_hash";

    private final String secret;
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public PortalLoginVerificationTokenService(
            @Value("${portal.login-verification.secret}") String secret,
            @Value("${portal.login-verification.ttl-seconds:300}") long ttlSeconds
    ) {
        this(secret, Duration.ofSeconds(ttlSeconds), Clock.systemUTC());
    }

    PortalLoginVerificationTokenService(String secret, Duration ttl, Clock clock) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Portal login verification secret must not be blank");
        }
        this.secret = secret;
        this.ttl = ttl;
        this.clock = clock;
    }

    public String issue(UUID userId, String portalType, String username, String password) {
        Instant now = clock.instant();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim(PORTAL_TYPE_CLAIM, normalize(portalType))
                .claim(USERNAME_HASH_CLAIM, hash(normalizeUsername(username)))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .setId(UUID.randomUUID().toString())
                .signWith(signingKey(portalType, username, password), SignatureAlgorithm.HS256)
                .compact();
    }

    public void verify(UUID userId, String portalType, String username, String password, String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey(portalType, username, password))
                    .setClock(() -> Date.from(clock.instant()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!userId.toString().equals(claims.getSubject())) {
                throw invalidToken();
            }
            if (!normalize(portalType).equals(claims.get(PORTAL_TYPE_CLAIM, String.class))) {
                throw invalidToken();
            }
            if (!hash(normalizeUsername(username)).equals(claims.get(USERNAME_HASH_CLAIM, String.class))) {
                throw invalidToken();
            }
        } catch (JwtException | IllegalArgumentException exception) {
            throw invalidToken();
        }
    }

    private Key signingKey(String portalType, String username, String password) {
        return Keys.hmacShaKeyFor(hashBytes(String.join("\n",
                secret,
                normalize(portalType),
                normalizeUsername(username),
                password == null ? "" : password
        )));
    }

    private String hash(String value) {
        return HexFormat.of().formatHex(hashBytes(value));
    }

    private byte[] hashBytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }

    private String normalize(String portalType) {
        return portalType == null ? "" : portalType.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private CommonException invalidToken() {
        return new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
}
