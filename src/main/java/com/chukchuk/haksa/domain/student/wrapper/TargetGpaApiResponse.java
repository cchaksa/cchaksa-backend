package com.chukchuk.haksa.domain.student.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 목표 평점 설정 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "TargetGpaApiResponse", description = "목표 GPA 설정 응답")
public class TargetGpaApiResponse extends SuccessResponse<MessageOnlyResponse> {

  /** 목표 평점 설정 성공 응답의 예시 값과 메시지를 구성한다. */
  public TargetGpaApiResponse() {
    super(new MessageOnlyResponse("목표 학점 저장 완료"), "요청 성공");
  }
}
