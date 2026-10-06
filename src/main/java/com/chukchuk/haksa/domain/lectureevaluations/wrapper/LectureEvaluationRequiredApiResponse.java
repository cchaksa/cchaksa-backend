package com.chukchuk.haksa.domain.lectureevaluations.wrapper;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 강의평가 대상 조회 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "LectureEvaluationRequiredApiResponse", description = "강의평가 상태 응답")
public class LectureEvaluationRequiredApiResponse
    extends SuccessResponse<LectureEvaluationDto.RequiredResponse> {

  /** 필수 강의평가 조회 성공 응답의 예시 값과 메시지를 구성한다. */
  public LectureEvaluationRequiredApiResponse() {
    super(
        new LectureEvaluationDto.RequiredResponse(
            LectureEvaluationStatus.PENDING, 2026, 10, List.of()),
        "요청 성공");
  }
}
