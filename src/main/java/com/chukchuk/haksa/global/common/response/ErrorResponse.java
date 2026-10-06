package com.chukchuk.haksa.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** 성공 여부가 항상 거짓인 공통 API 오류 응답을 전달한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Schema(description = "API 실패 응답 포맷")
public class ErrorResponse {

  @Schema(description = "성공 여부", example = "false", required = true)
  private final boolean success = false;

  @Schema(description = "에러 상세")
  private final ErrorDetail error;

  /**
   * 오류 상세 정보로 API 실패 응답을 생성한다.
   *
   * @param error 오류 코드, 메시지 및 선택 상세 정보
   */
  protected ErrorResponse(ErrorDetail error) {
    this.error = error;
  }

  /**
   * 오류 코드와 메시지만 포함한 API 실패 응답을 생성한다.
   *
   * @param code 클라이언트가 분기 처리할 애플리케이션 오류 코드
   * @param message 응답 메시지
   * @return 추가 상세 정보가 없는 API 오류 응답
   */
  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new ErrorDetail(code, message, null));
  }

  /**
   * 오류 코드·메시지와 추가 상세 정보를 포함한 API 실패 응답을 생성한다.
   *
   * @param code 클라이언트가 분기 처리할 애플리케이션 오류 코드
   * @param message 응답 메시지
   * @param details 상세 정보
   * @return 호출자가 제공한 추가 상세 정보를 포함한 API 오류 응답
   */
  public static ErrorResponse of(String code, String message, Object details) {
    return new ErrorResponse(new ErrorDetail(code, message, details));
  }
}
