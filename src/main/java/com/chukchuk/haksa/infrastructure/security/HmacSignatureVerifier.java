package com.chukchuk.haksa.infrastructure.security;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class HmacSignatureVerifier {

    public enum VerificationFailureReason {
        OK,
        MISSING_TIMESTAMP,
        MISSING_BODY,
        MISSING_SIGNATURE,
        MISSING_SECRET,
        INVALID_TIMESTAMP,
        TIMESTAMP_SKEW_EXCEEDED,
        SIGNATURE_MISMATCH
    }

    public record VerificationResult(
            boolean valid,
            VerificationFailureReason reason
    ) {}

    private final String secret;
    private final long allowedSkewSeconds;

    @Autowired
    public HmacSignatureVerifier(ScrapingProperties scrapingProperties) {
        this(scrapingProperties.getCallback().getHmacSecret(), scrapingProperties.getCallback().getAllowedSkewSeconds());
    }

    public HmacSignatureVerifier(String secret, long allowedSkewSeconds) {
        this.secret = secret;
        this.allowedSkewSeconds = allowedSkewSeconds;
    }

    public void verify(String timestamp, String rawBody, String signature) {
        VerificationResult result = inspect(timestamp, rawBody, signature);
        if (!result.valid()) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
        }
    }

    public VerificationResult inspect(String timestamp, String rawBody, String signature) {
        if (isBlank(timestamp)) {
            return invalid(VerificationFailureReason.MISSING_TIMESTAMP);
        }
        if (isBlank(rawBody)) {
            return invalid(VerificationFailureReason.MISSING_BODY);
        }
        if (isBlank(signature)) {
            return invalid(VerificationFailureReason.MISSING_SIGNATURE);
        }
        if (isBlank(secret)) {
            return invalid(VerificationFailureReason.MISSING_SECRET);
        }

        Instant parsedTimestamp;
        try {
            parsedTimestamp = parseTimestamp(timestamp);
        } catch (CommonException exception) {
            return invalid(VerificationFailureReason.INVALID_TIMESTAMP);
        }

        long delta = Math.abs(Instant.now().getEpochSecond() - parsedTimestamp.getEpochSecond());
        if (delta > allowedSkewSeconds) {
            return invalid(VerificationFailureReason.TIMESTAMP_SKEW_EXCEEDED);
        }

        byte[] expected = hmac(timestamp + "." + rawBody);
        if (!matches(expected, signature)) {
            return invalid(VerificationFailureReason.SIGNATURE_MISMATCH);
        }

        return new VerificationResult(true, VerificationFailureReason.OK);
    }

    public byte[] hmac(String canonicalString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKeyBytes(), "HmacSHA256"));
            return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, e);
        }
    }

    private boolean matches(byte[] expected, String signature) {
        return MessageDigest.isEqual(expected, decode(signature));
    }

    private byte[] decode(String signature) {
        String normalizedSignature = normalizeSignature(signature);
        if (looksLikeHex(normalizedSignature)) {
            return hexToBytes(normalizedSignature);
        }
        try {
            return Base64.getDecoder().decode(normalizedSignature);
        } catch (IllegalArgumentException ignored) {
            try {
                return Base64.getUrlDecoder().decode(normalizedSignature);
            } catch (IllegalArgumentException ignoredAgain) {
                return hexToBytes(normalizedSignature);
            }
        }
    }

    private byte[] secretKeyBytes() {
        if (looksLikeHex(secret)) {
            return HexFormat.of().parseHex(secret);
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private String normalizeSignature(String signature) {
        String trimmed = signature.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("sha256=")) {
            return trimmed.substring("sha256=".length());
        }
        return trimmed;
    }

    private boolean looksLikeHex(String value) {
        if (value == null || value.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private byte[] hexToBytes(String value) {
        if (value.length() % 2 != 0) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
        }

        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
            }
            bytes[i / 2] = (byte) ((high << 4) + low);
        }
        return bytes;
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.parse(timestamp);
        } catch (DateTimeParseException ignored) {
            try {
                long value = Long.parseLong(timestamp);
                return timestamp.length() > 10 ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
            } catch (NumberFormatException ex) {
                throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, ex);
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private VerificationResult invalid(VerificationFailureReason reason) {
        return new VerificationResult(false, reason);
    }
}
