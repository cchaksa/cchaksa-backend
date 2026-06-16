// 포털 로그인 검증 token을 stateless 방식으로 발급하고 검증하는 서비스
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PortalLoginVerificationTokenService {

    private static final String VERSION = "v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

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
        String canonical = canonicalPayload(userId, portalType, username, password);
        return encode(canonical) + "." + encode(hmac(canonical));
    }

    public void verify(UUID userId, String portalType, String username, String password, String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalidToken();
        }

        String canonical = decodeToString(parts[0]);
        byte[] expectedSignature = hmac(canonical);
        byte[] actualSignature = decodeToBytes(parts[1]);
        if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
            throw invalidToken();
        }

        String[] fields = canonical.split("\\|", -1);
        if (fields.length != 7) {
            throw invalidToken();
        }
        if (!VERSION.equals(fields[0])) {
            throw invalidToken();
        }
        if (!userId.toString().equals(fields[1])) {
            throw invalidToken();
        }
        if (!normalize(portalType).equals(fields[2])) {
            throw invalidToken();
        }
        if (!hash(normalizeUsername(username)).equals(fields[3])) {
            throw invalidToken();
        }
        if (!credentialFingerprint(portalType, username, password).equals(fields[4])) {
            throw invalidToken();
        }
        if (clock.instant().getEpochSecond() > parseExpiresAt(fields[5])) {
            throw invalidToken();
        }
    }

    private String canonicalPayload(UUID userId, String portalType, String username, String password) {
        long expiresAt = clock.instant().plus(ttl).getEpochSecond();
        return String.join("|",
                VERSION,
                userId.toString(),
                normalize(portalType),
                hash(normalizeUsername(username)),
                credentialFingerprint(portalType, username, password),
                String.valueOf(expiresAt),
                UUID.randomUUID().toString()
        );
    }

    private String credentialFingerprint(String portalType, String username, String password) {
        return hash(String.join("\n", normalize(portalType), normalizeUsername(username), password));
    }

    private long parseExpiresAt(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw invalidToken();
        }
    }

    private byte[] hmac(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, exception);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, exception);
        }
    }

    private String encode(String value) {
        return encode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String decodeToString(String value) {
        return new String(decodeToBytes(value), StandardCharsets.UTF_8);
    }

    private byte[] decodeToBytes(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
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
