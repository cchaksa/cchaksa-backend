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

@Component
public class HmacSignatureVerifier {

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
        if (isBlank(timestamp) || isBlank(rawBody) || isBlank(signature) || isBlank(secret)) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
        }

        Instant parsedTimestamp = parseTimestamp(timestamp);
        long delta = Math.abs(Instant.now().getEpochSecond() - parsedTimestamp.getEpochSecond());
        if (delta > allowedSkewSeconds) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
        }

        byte[] expected = hmac(timestamp + "." + rawBody);
        if (!matches(expected, signature)) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
        }
    }

    public byte[] hmac(String canonicalString) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, e);
        }
    }

    private boolean matches(byte[] expected, String signature) {
        return MessageDigest.isEqual(expected, decode(signature));
    }

    private byte[] decode(String signature) {
        try {
            return Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException ignored) {
            return hexToBytes(signature);
        }
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
}
