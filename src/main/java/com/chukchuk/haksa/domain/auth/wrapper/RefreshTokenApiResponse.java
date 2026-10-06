package com.chukchuk.haksa.domain.auth.wrapper;

import com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 인증 토큰 재발급 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "RefreshTokenApiResponse", description = "토큰 재발급 응답 포맷")
public class RefreshTokenApiResponse extends SuccessResponse<RefreshResponse> {

  /** 인증 토큰 재발급 성공 응답의 예시 값과 메시지를 구성한다. */
  public RefreshTokenApiResponse() {
    super(new RefreshResponse("new-access-token", "new-refresh-token"), "요청 성공");
  }
}
