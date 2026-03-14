package com.chukchuk.haksa.infrastructure.security;

import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class HmacSignatureVerifierUnitTests {

    @Test
    @DisplayName("hex 문자열 secret으로 생성한 hex signature를 검증한다")
    void verify_acceptsHexSecretAndHexSignature() throws Exception {
        String secret = "a89f20be2f4a38ee011f98d6e7ef1290fd100e82ca4605ea923c81ba80a0d35ed64ad6746f6e159cd59d1864e60cfbb8cdbdf98c79d163c8526916f9beed3a6e";
        String timestamp = Instant.now().toString();
        String rawBody = "{\"job_id\":\"job-1\",\"status\":\"failed\"}";
        String signature = signHex(secret, timestamp + "." + rawBody);

        HmacSignatureVerifier verifier = new HmacSignatureVerifier(secret, 300);

        assertThatCode(() -> verifier.verify(timestamp, rawBody, signature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("hex 형태 secret이어도 원문 UTF-8 secret으로 만든 signature를 허용한다")
    void verify_acceptsUtf8SignatureWhenSecretLooksHex() throws Exception {
        String secret = "a89f20be2f4a38ee011f98d6e7ef1290fd100e82ca4605ea923c81ba80a0d35ed64ad6746f6e159cd59d1864e60cfbb8cdbdf98c79d163c8526916f9beed3a6e";
        String timestamp = Instant.now().toString();
        String rawBody = "{\"job_id\":\"job-1\",\"status\":\"failed\"}";
        String signature = signBase64(secret.getBytes(StandardCharsets.UTF_8), timestamp + "." + rawBody);

        HmacSignatureVerifier verifier = new HmacSignatureVerifier(secret, 300);

        assertThatCode(() -> verifier.verify(timestamp, rawBody, signature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sha256= prefix가 붙은 signature도 검증한다")
    void verify_acceptsPrefixedSignature() throws Exception {
        String secret = "test-callback-secret";
        String timestamp = Instant.now().toString();
        String rawBody = "{\"job_id\":\"job-1\",\"status\":\"failed\"}";
        String signature = "sha256=" + signBase64(secret.getBytes(StandardCharsets.UTF_8), timestamp + "." + rawBody);

        HmacSignatureVerifier verifier = new HmacSignatureVerifier(secret, 300);

        assertThatCode(() -> verifier.verify(timestamp, rawBody, signature))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("검증 실패 사유를 구체적으로 반환한다")
    void inspect_returnsDetailedReason() {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("test-callback-secret", 300);

        HmacSignatureVerifier.VerificationResult result = verifier.inspect("", "{}", "signature");

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).isEqualTo(HmacSignatureVerifier.VerificationFailureReason.MISSING_TIMESTAMP);
    }

    @Test
    @DisplayName("diagnostics는 signature mismatch 시 비교 정보를 제공한다")
    void diagnostics_exposesSignatureComparisonHints() {
        HmacSignatureVerifier verifier = new HmacSignatureVerifier("test-callback-secret", 300);

        HmacSignatureVerifier.VerificationDiagnostics diagnostics = verifier.diagnostics(
                String.valueOf(Instant.now().toEpochMilli()),
                "{\"job_id\":\"job-1\"}",
                "invalid-signature"
        );

        assertThat(diagnostics.reason()).isNotEqualTo(HmacSignatureVerifier.VerificationFailureReason.OK);
        assertThat(diagnostics.actualSignatureEncoding()).isNotBlank();
        assertThat(diagnostics.rawBodyHash()).isNotBlank();
        assertThat(diagnostics.expectedUtf8SignatureHash()).isNotBlank();
    }

    private static String signHex(String secretHex, String canonicalString) throws Exception {
        return HexFormat.of().formatHex(hmac(HexFormat.of().parseHex(secretHex), canonicalString));
    }

    private static String signBase64(byte[] secretBytes, String canonicalString) throws Exception {
        return Base64.getEncoder().encodeToString(hmac(secretBytes, canonicalString));
    }

    private static byte[] hmac(byte[] secretBytes, String canonicalString) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
        return mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
    }
}
