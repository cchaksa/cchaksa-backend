// 내 사용자 정보 조회 API의 문서용 응답 래퍼

package com.chukchuk.haksa.domain.user.wrapper;

import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 내 사용자 정보 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "MeApiResponse", description = "내 사용자 정보 조회 응답")
public class MeApiResponse extends SuccessResponse<UserDto.MeResponse> {

  /** 내 사용자 정보 조회 성공 응답의 예시 값과 메시지를 구성한다. */
  public MeApiResponse() {
    super(new UserDto.MeResponse(true), "요청 성공");
  }
}
