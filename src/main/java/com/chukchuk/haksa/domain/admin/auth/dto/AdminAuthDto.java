package com.chukchuk.haksa.domain.admin.auth.dto;

import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** 관리자 인증 API의 요청과 응답 계약이다. */
public final class AdminAuthDto {
  private AdminAuthDto() {}

  /** 서버가 발급한 로그인 challenge다. */
  public record ChallengeResponse(UUID challengeId, String nonce) {}

  /** 카카오 ID Token을 제출하는 로그인 요청이다. */
  public record SignInRequest(@NotNull UUID challengeId, @NotBlank String idToken) {}

  /** 인증된 관리자에게 노출할 최소 계정 정보다. */
  public record AdminResponse(UUID adminAccountId, String displayName, AdminRole adminRole) {}
}
