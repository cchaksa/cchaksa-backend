// 문의 상세 성공 응답의 OpenAPI 스키마를 제공한다.

package com.chukchuk.haksa.domain.report.wrapper;

import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 문의 상세 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "ReportDetailApiResponse", description = "문의 상세 응답")
public class ReportDetailApiResponse extends SuccessResponse<ReportDto.DetailResponse> {

  /** 문의 상세 응답 예시를 구성한다. */
  public ReportDetailApiResponse() {
    super(
        new ReportDto.DetailResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            ReportStatus.ANSWERED,
            "졸업 요건 확인이 불가능합니다.",
            "졸업 요건 화면을 확인할 수 없습니다.",
            Instant.parse("2026-09-22T00:00:00Z"),
            "학적 정보를 다시 연동해 주세요.",
            Instant.parse("2026-09-22T01:00:00Z")),
        "요청 성공");
  }
}
