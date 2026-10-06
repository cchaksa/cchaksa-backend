package com.chukchuk.haksa.domain.lectureevaluations.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 강의평가 제출 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "LectureEvaluationSubmitApiResponse", description = "강의평가 제출 응답")
public class LectureEvaluationSubmitApiResponse extends SuccessResponse<MessageOnlyResponse> {

  /** 강의평가 제출 성공 응답의 예시 값과 메시지를 구성한다. */
  public LectureEvaluationSubmitApiResponse() {
    super(new MessageOnlyResponse("강의평가 저장 완료"), "요청 성공");
  }
}
