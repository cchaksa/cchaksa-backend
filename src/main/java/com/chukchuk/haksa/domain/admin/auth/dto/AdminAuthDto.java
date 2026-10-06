package com.chukchuk.haksa.domain.admin.auth.dto;

import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** 관리자 인증 API의 요청과 응답 계약이다. */
public final class AdminAuthDto {
  private AdminAuthDto() {}

  /** 관리자 로컬 자격증명을 제출하는 로그인 요청이다. */
  @Schema(name = "AdminAuthSignInRequest")
  public record SignInRequest(
      @NotBlank @Size(max = 255) String loginId, @NotBlank @Size(max = 256) String password) {}

  /** 현재 비밀번호를 검증하고 새 비밀번호로 교체하는 요청이다. */
  @Schema(name = "AdminPasswordChangeRequest")
  public record PasswordChangeRequest(
      @NotBlank @Size(max = 256) String currentPassword,
      @NotBlank @Size(max = 256) String newPassword) {}

  /** 인증된 관리자에게 노출할 최소 계정 정보다. */
  @Schema(name = "AdminAuthResponse")
  public record AdminResponse(UUID adminAccountId, String displayName, AdminRole adminRole) {}
}
