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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
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

    public record VerificationDiagnostics(
            VerificationFailureReason reason,
            String parsedTimestamp,
            Long timestampDeltaSeconds,
            String rawBodyHash,
            String actualSignatureEncoding,
            Integer actualSignatureLength,
            String actualSignatureHash,
            String expectedUtf8SignatureHash,
            String expectedHexSignatureHash
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

        byte[] actualSignature = decode(signature);
        if (!matches(timestamp + "." + rawBody, actualSignature)) {
            return invalid(VerificationFailureReason.SIGNATURE_MISMATCH);
        }

        return new VerificationResult(true, VerificationFailureReason.OK);
    }

    public VerificationDiagnostics diagnostics(String timestamp, String rawBody, String signature) {
        VerificationFailureReason reason;
        Instant parsedTimestamp = null;
        Long delta = null;

        if (isBlank(timestamp)) {
            reason = VerificationFailureReason.MISSING_TIMESTAMP;
        } else if (isBlank(rawBody)) {
            reason = VerificationFailureReason.MISSING_BODY;
        } else if (isBlank(signature)) {
            reason = VerificationFailureReason.MISSING_SIGNATURE;
        } else if (isBlank(secret)) {
            reason = VerificationFailureReason.MISSING_SECRET;
        } else {
            try {
                parsedTimestamp = parseTimestamp(timestamp);
                delta = Math.abs(Instant.now().getEpochSecond() - parsedTimestamp.getEpochSecond());
                if (delta > allowedSkewSeconds) {
                    reason = VerificationFailureReason.TIMESTAMP_SKEW_EXCEEDED;
                } else {
                    byte[] actualSignature = decode(signature);
                    reason = matches(timestamp + "." + rawBody, actualSignature)
                            ? VerificationFailureReason.OK
                            : VerificationFailureReason.SIGNATURE_MISMATCH;
                }
            } catch (CommonException exception) {
                reason = VerificationFailureReason.INVALID_TIMESTAMP;
            }
        }

        byte[] actualBytes = safeDecode(signature);
        byte[] expectedUtf8 = safeHmac(timestamp + "." + rawBody, secret.getBytes(StandardCharsets.UTF_8));
        byte[] expectedHex = looksLikeHex(secret)
                ? safeHmac(timestamp + "." + rawBody, HexFormat.of().parseHex(secret))
                : null;

        return new VerificationDiagnostics(
                reason,
                parsedTimestamp == null ? null : parsedTimestamp.toString(),
                delta,
                hash(rawBody),
                detectSignatureEncoding(signature),
                actualBytes == null ? null : actualBytes.length,
                hash(actualBytes),
                hash(expectedUtf8),
                hash(expectedHex)
        );
    }

    public byte[] hmac(String canonicalString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(preferredSecretKeyBytes(), "HmacSHA256"));
            return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, e);
        }
    }

    private boolean matches(String canonicalString, byte[] actualSignature) {
        for (byte[] secretBytes : candidateSecretKeyBytes()) {
            if (MessageDigest.isEqual(hmac(canonicalString, secretBytes), actualSignature)) {
                return true;
            }
        }
        return false;
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

    private byte[] preferredSecretKeyBytes() {
        if (looksLikeHex(secret)) {
            return HexFormat.of().parseHex(secret);
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private List<byte[]> candidateSecretKeyBytes() {
        List<byte[]> candidates = new ArrayList<>();
        candidates.add(secret.getBytes(StandardCharsets.UTF_8));
        if (looksLikeHex(secret)) {
            byte[] hexBytes = HexFormat.of().parseHex(secret);
            if (!MessageDigest.isEqual(hexBytes, candidates.get(0))) {
                candidates.add(hexBytes);
            }
        }
        return candidates;
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

    private byte[] hmac(String canonicalString, byte[] secretBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, e);
        }
    }

    private byte[] safeDecode(String signature) {
        try {
            return isBlank(signature) ? null : decode(signature);
        } catch (CommonException exception) {
            return null;
        }
    }

    private byte[] safeHmac(String canonicalString, byte[] secretBytes) {
        try {
            return hmac(canonicalString, secretBytes);
        } catch (CommonException exception) {
            return null;
        }
    }

    private String hash(String value) {
        if (value == null) {
            return null;
        }
        return hash(value.getBytes(StandardCharsets.UTF_8));
    }

    private String hash(byte[] value) {
        if (value == null) {
            return null;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String detectSignatureEncoding(String signature) {
        if (isBlank(signature)) {
            return null;
        }
        String normalized = normalizeSignature(signature);
        if (looksLikeHex(normalized)) {
            return "hex";
        }
        try {
            Base64.getDecoder().decode(normalized);
            return "base64";
        } catch (IllegalArgumentException ignored) {
            try {
                Base64.getUrlDecoder().decode(normalized);
                return "base64url";
            } catch (IllegalArgumentException ignoredAgain) {
                return "unknown";
            }
        }
    }
}
