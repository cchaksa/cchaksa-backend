package com.chukchuk.haksa.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/** 성공 여부가 항상 참인 공통 API 응답과 선택 메시지를 전달한다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Schema(description = "성공 응답 포맷")
public class SuccessResponse<T> {

  @Schema(description = "성공 여부", example = "true", required = true)
  private final boolean success = true;

  @Schema(description = "응답 데이터", required = true)
  private final T data;

  @Schema(description = "메시지", example = "요청 성공")
  private final String message;

  /**
   * 응답 데이터와 호출자에게 표시할 메시지로 성공 응답을 생성한다.
   *
   * @param data 응답 데이터
   * @param message 응답 메시지
   */
  public SuccessResponse(T data, String message) {
    this.data = data;
    this.message = message;
  }

  /**
   * 지정한 데이터와 메시지로 성공 API 응답을 생성한다.
   *
   * @param <T> 응답 데이터 유형
   * @param data 응답 데이터
   * @param message 응답 메시지
   * @return 지정한 데이터와 메시지를 포함한 성공 응답
   */
  public static <T> SuccessResponse<T> of(T data, String message) {
    return new SuccessResponse<>(data, message);
  }

  /**
   * 지정한 데이터와 기본 성공 메시지로 API 응답을 생성한다.
   *
   * @param <T> 응답 데이터 유형
   * @param data 응답 데이터
   * @return 기본 메시지 {@code 요청 성공}을 포함한 성공 응답
   */
  public static <T> SuccessResponse<T> of(T data) {
    return new SuccessResponse<>(data, "요청 성공");
  }
}
