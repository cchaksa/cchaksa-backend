// 리프레시 토큰 해시 계산을 검증하는 테스트
package com.chukchuk.haksa.domain.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHasherTests {

    @Test
    void hash_returnsDeterministicSecretBasedValue() {
        RefreshTokenHasher hasher = new RefreshTokenHasher("01234567890123456789012345678901");
        RefreshTokenHasher otherSecretHasher = new RefreshTokenHasher("abcdefghijklmnopqrstuvwxyz123456");

        String first = hasher.hash("refresh-token");
        String second = hasher.hash("refresh-token");
        String otherSecret = otherSecretHasher.hash("refresh-token");

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo("refresh-token");
        assertThat(first).isNotEqualTo(otherSecret);
    }

    @Test
    void matches_comparesRawTokenWithStoredHash() {
        RefreshTokenHasher hasher = new RefreshTokenHasher("01234567890123456789012345678901");
        String tokenHash = hasher.hash("refresh-token");

        assertThat(hasher.matches("refresh-token", tokenHash)).isTrue();
        assertThat(hasher.matches("other-token", tokenHash)).isFalse();
        assertThat(hasher.matches("refresh-token", null)).isFalse();
    }
}
