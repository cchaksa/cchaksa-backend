package com.chukchuk.haksa.domain.academic.record.wrapper;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

/** 학기별 성적 목록 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "SemesterGradesApiResponse", description = "학기별 성적 목록 응답")
public class SemesterGradesApiResponse extends SuccessResponse<List<SemesterSummaryResponse>> {
  /** 빈 학기별 성적 목록을 담은 문서용 성공 응답을 생성한다. */
  public SemesterGradesApiResponse() {
    super(Collections.emptyList(), "요청 성공");
  }
}
