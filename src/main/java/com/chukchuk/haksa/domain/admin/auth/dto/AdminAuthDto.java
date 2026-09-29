package com.chukchuk.haksa.domain.admin.auth.dto;

import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** 관리자 인증 API의 요청과 응답 계약이다. */
public final class AdminAuthDto {
  private AdminAuthDto() {}

  /** 서버가 발급한 로그인 challenge다. */
  @Schema(name = "AdminAuthChallengeResponse")
  public record ChallengeResponse(
      UUID challengeId, String nonce, String state, String javascriptAppKey, String redirectUri) {}

  /** 카카오 authorization code를 제출하는 로그인 요청이다. */
  @Schema(name = "AdminAuthSignInRequest")
  public record SignInRequest(
      @NotNull UUID challengeId,
      @NotBlank @Size(max = 2048) String authorizationCode,
      @NotBlank @Size(max = 128) String state) {}

  /** 인증된 관리자에게 노출할 최소 계정 정보다. */
  @Schema(name = "AdminAuthResponse")
  public record AdminResponse(UUID adminAccountId, String displayName, AdminRole adminRole) {}
}
