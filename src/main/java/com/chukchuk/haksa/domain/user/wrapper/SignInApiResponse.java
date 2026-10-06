package com.chukchuk.haksa.domain.user.wrapper;

import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** OIDC 회원가입 및 로그인 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "SignInApiResponse", description = "회원가입 및 로그인 응답")
public class SignInApiResponse extends SuccessResponse<UserDto.SignInResponse> {

  /** OIDC 회원가입 및 로그인 성공 응답의 예시 값과 메시지를 구성한다. */
  public SignInApiResponse() {
    super(
        new UserDto.SignInResponse(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
            true),
        "요청 성공");
  }
}
