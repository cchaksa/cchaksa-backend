package com.chukchuk.haksa.global.common.response.wrapper;

import com.chukchuk.haksa.global.common.response.ErrorDetail;
import com.chukchuk.haksa.global.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** Springdoc에 공통 오류 응답 타입을 노출한다. */
@Schema(name = "ErrorResponseWrapper", description = "API 에러 응답 포맷")
public class ErrorResponseWrapper extends ErrorResponse {

  /** Springdoc schema 생성을 위한 예시 오류 상세 정보로 wrapper를 생성한다. */
  public ErrorResponseWrapper() {
    super(new ErrorDetail("ERROR_CODE", "에러 메시지", null));
  }
}
