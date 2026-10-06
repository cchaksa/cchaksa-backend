// 사용자 분석 식별자 조회 API의 문서용 응답 래퍼

package com.chukchuk.haksa.domain.user.wrapper;

import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 분석 사용자 식별자 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "AnalyticsIdApiResponse", description = "사용자 분석 식별자 조회 응답")
public class AnalyticsIdApiResponse extends SuccessResponse<UserDto.AnalyticsIdResponse> {

  /** 분석 사용자 식별자 조회 성공 응답의 예시 값과 메시지를 구성한다. */
  public AnalyticsIdApiResponse() {
    super(new UserDto.AnalyticsIdResponse("550e8400-e29b-41d4-a716-446655440000"), "요청 성공");
  }
}
