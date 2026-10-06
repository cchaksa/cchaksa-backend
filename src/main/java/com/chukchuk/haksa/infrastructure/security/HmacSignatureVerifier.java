package com.chukchuk.haksa.infrastructure.security;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 스크래핑 콜백의 시각 오차와 HMAC-SHA256 서명을 검증한다. */
@Component
public class HmacSignatureVerifier {

  /** 콜백 서명 검증 성공 또는 실패 원인을 구분한다. */
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

  /**
   * 콜백 서명의 유효 여부와 최초 실패 원인을 전달한다.
   *
   * @param valid timestamp와 HMAC 서명이 모두 유효한지 여부
   * @param reason 검증 성공 또는 최초 실패 원인
   */
  public record VerificationResult(boolean valid, VerificationFailureReason reason) {}

  /**
   * 원문이나 비밀키를 노출하지 않고 콜백 서명 실패를 분석할 값을 전달한다.
   *
   * @param reason 검증 성공 또는 최초 실패 원인
   * @param parsedTimestamp 파싱된 요청 시각의 ISO-8601 표현
   * @param timestampDeltaSeconds 서버 현재 시각과 요청 시각의 차이(초)
   * @param rawBodyHash 요청 본문의 SHA-256 해시
   * @param actualSignatureEncoding 전달된 서명의 감지된 인코딩
   * @param actualSignatureLength 디코딩한 서명의 byte 길이
   * @param actualSignatureHash 전달된 서명의 SHA-256 해시
   * @param expectedUtf8SignatureHash 비밀키를 UTF-8로 해석한 예상 서명의 해시
   * @param expectedHexSignatureHash 비밀키를 16진수로 해석한 예상 서명의 해시
   */
  public record VerificationDiagnostics(
      VerificationFailureReason reason,
      String parsedTimestamp,
      Long timestampDeltaSeconds,
      String rawBodyHash,
      String actualSignatureEncoding,
      Integer actualSignatureLength,
      String actualSignatureHash,
      String expectedUtf8SignatureHash,
      String expectedHexSignatureHash) {}

  private final String secret;
  private final long allowedSkewSeconds;

  /**
   * 스크래핑 콜백 설정으로 서명 검증기를 생성한다.
   *
   * @param scrapingProperties HMAC 비밀키와 허용 시각 오차 설정
   */
  @Autowired
  public HmacSignatureVerifier(ScrapingProperties scrapingProperties) {
    this(
        scrapingProperties.getCallback().getHmacSecret(),
        scrapingProperties.getCallback().getAllowedSkewSeconds());
  }

  /**
   * HMAC secret과 허용 시각 오차로 서명 검증기를 생성한다.
   *
   * @param secret HMAC-SHA256 비밀키의 UTF-8 또는 16진수 표현
   * @param allowedSkewSeconds replay 방지를 위해 허용할 요청 시각 오차(초)
   */
  public HmacSignatureVerifier(String secret, long allowedSkewSeconds) {
    this.secret = secret;
    this.allowedSkewSeconds = allowedSkewSeconds;
  }

  /**
   * 요청 timestamp와 본문을 결합한 HMAC 서명 및 허용 시각 오차를 검증한다.
   *
   * @param timestamp 요청 타임스탬프
   * @param rawBody 서명 검증 대상 요청 본문
   * @param signature Base64, Base64 URL 또는 16진수로 표현된 요청 서명
   * @throws CommonException 필수 값, timestamp 또는 서명이 유효하지 않은 경우
   */
  public void verify(String timestamp, String rawBody, String signature) {
    VerificationResult result = inspect(timestamp, rawBody, signature);
    if (!result.valid()) {
      throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE);
    }
  }

  /**
   * HMAC 서명을 검증하고 판정 결과를 반환한다.
   *
   * @param timestamp 요청 타임스탬프
   * @param rawBody 서명 검증 대상 요청 본문
   * @param signature 요청 서명
   * @return 예외를 발생시키지 않는 검증 결과와 실패 원인
   */
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

  /**
   * HMAC 서명 검증에 필요한 진단 정보를 반환한다.
   *
   * @param timestamp 요청 타임스탬프
   * @param rawBody 서명 검증 대상 요청 본문
   * @param signature 요청 서명
   * @return 원문과 비밀키 대신 길이·인코딩·해시로 구성한 진단 정보
   */
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
          reason =
              matches(timestamp + "." + rawBody, actualSignature)
                  ? VerificationFailureReason.OK
                  : VerificationFailureReason.SIGNATURE_MISMATCH;
        }
      } catch (CommonException exception) {
        reason = VerificationFailureReason.INVALID_TIMESTAMP;
      }
    }

    byte[] actualBytes = safeDecode(signature);
    byte[] expectedUtf8 =
        safeHmac(timestamp + "." + rawBody, secret.getBytes(StandardCharsets.UTF_8));
    byte[] expectedHex =
        looksLikeHex(secret)
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
        hash(expectedHex));
  }

  /**
   * 정규화된 문자열의 HMAC 값을 계산한다.
   *
   * @param canonicalString timestamp와 본문을 결합한 서명 대상 문자열
   * @return 설정된 비밀키로 계산한 HMAC-SHA256 byte 배열
   * @throws CommonException 비밀키 또는 HMAC 계산을 사용할 수 없는 경우
   */
  public byte[] hmac(String canonicalString) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(preferredSecretKeyBytes(), "HmacSHA256"));
      return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE, e);
    }
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
