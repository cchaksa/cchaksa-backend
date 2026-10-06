// 문의 생성 성공 응답의 OpenAPI 스키마를 제공한다.

package com.chukchuk.haksa.domain.report.wrapper;

import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** 문의 생성 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "ReportCreateApiResponse", description = "문의 생성 응답")
public class ReportCreateApiResponse extends SuccessResponse<ReportDto.CreateResponse> {

  /** 문의 생성 응답 예시를 구성한다. */
  public ReportCreateApiResponse() {
    super(
        new ReportDto.CreateResponse(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            ReportStatus.PENDING,
            Instant.parse("2026-09-22T00:00:00Z")),
        "요청 성공");
  }
}
