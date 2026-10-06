package com.chukchuk.haksa.domain.academic.record.wrapper;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 누적 학업 요약 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "AcademicSummaryApiResponse", description = "학업 요약 정보 응답")
public class AcademicSummaryApiResponse extends SuccessResponse<AcademicSummaryResponse> {

  /** 모든 누적 학업 지표가 0인 문서용 성공 응답을 생성한다. */
  public AcademicSummaryApiResponse() {
    super(new AcademicSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, 0), "요청 성공");
  }
}
