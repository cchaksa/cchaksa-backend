// 포털 로그인 검증 token의 발급과 검증 규칙을 검증하는 테스트
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortalLoginVerificationTokenServiceUnitTests {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-06-16T10:00:00Z");

    @Test
    @DisplayName("발급한 verification token은 같은 사용자와 같은 자격 증명에서 검증된다")
    void verify_acceptsMatchingUserAndCredential() {
        UUID userId = UUID.randomUUID();
        PortalLoginVerificationTokenService service = serviceAt(NOW);

        String token = service.issue(userId, "suwon", "17019013", "pw");

        service.verify(userId, "suwon", "17019013", "pw", token);
    }

    @Test
    @DisplayName("다른 비밀번호로 제출한 verification token은 거부한다")
    void verify_rejectsDifferentPassword() {
        UUID userId = UUID.randomUUID();
        PortalLoginVerificationTokenService service = serviceAt(NOW);
        String token = service.issue(userId, "suwon", "17019013", "pw");

        assertThatThrownBy(() -> service.verify(userId, "suwon", "17019013", "wrong", token))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT.code()));
    }

    @Test
    @DisplayName("만료된 verification token은 거부한다")
    void verify_rejectsExpiredToken() {
        UUID userId = UUID.randomUUID();
        PortalLoginVerificationTokenService issuer = serviceAt(NOW);
        String token = issuer.issue(userId, "suwon", "17019013", "pw");
        PortalLoginVerificationTokenService verifier = serviceAt(NOW.plusSeconds(301));

        assertThatThrownBy(() -> verifier.verify(userId, "suwon", "17019013", "pw", token))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.INVALID_ARGUMENT.code()));
    }

    private PortalLoginVerificationTokenService serviceAt(Instant instant) {
        return new PortalLoginVerificationTokenService(SECRET, Duration.ofMinutes(5), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
