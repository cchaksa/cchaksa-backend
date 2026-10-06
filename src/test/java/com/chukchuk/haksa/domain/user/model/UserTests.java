// 사용자 병합 시 탈퇴 상태 전파 방지를 검증하는 도메인 테스트

package com.chukchuk.haksa.domain.user.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTests {

  @Test
  @DisplayName("탈퇴한 원본을 병합해도 대상 사용자는 활성 상태를 유지한다")
  void absorbFromWithdrawnUserKeepsTargetActive() {
    User target = User.builder().email("target@example.com").profileNickname("target").build();
    User withdrawnOrigin =
        User.builder().email("origin@example.com").profileNickname("origin").build();
    withdrawnOrigin.withdraw(Instant.now());

    target.absorbFrom(withdrawnOrigin);

    assertThat(target.getIsDeleted()).isFalse();
    assertThat(target.getDeletedAt()).isNull();
  }
}
