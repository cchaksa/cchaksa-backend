// 문의 목록 성공 응답의 OpenAPI 스키마를 제공한다.

package com.chukchuk.haksa.domain.report.wrapper;

import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 문의 목록 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "ReportListApiResponse", description = "문의 목록 응답")
public class ReportListApiResponse extends SuccessResponse<ReportDto.PageResponse> {

  /** 문의 목록 응답 예시를 구성한다. */
  public ReportListApiResponse() {
    super(
        new ReportDto.PageResponse(
            List.of(
                new ReportDto.ListItem(
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    ReportStatus.PENDING,
                    "졸업 요건 확인이 불가능합니다.",
                    Instant.parse("2026-09-22T00:00:00Z"),
                    null)),
            0,
            20,
            1,
            1,
            false),
        "요청 성공");
  }
}
