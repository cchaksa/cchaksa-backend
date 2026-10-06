// 포털 링크 job 요약 조회 성공 응답의 Swagger 문서용 래퍼

package com.chukchuk.haksa.domain.portal.wrapper;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 포털 연동 작업 요약 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "PortalLinkJobSummaryApiResponse", description = "포털 링크 job 요약 조회 응답")
public class PortalLinkJobSummaryApiResponse
    extends SuccessResponse<PortalLinkDto.JobSummaryResponse> {

  /** 포털 연동 작업 요약 조회 성공 응답의 예시 값과 메시지를 구성한다. */
  public PortalLinkJobSummaryApiResponse() {
    super(
        new PortalLinkDto.JobSummaryResponse(
            "job-123",
            "succeeded",
            new PortalLinkDto.StudentInfoSummary("홍길동", "수원대학교", "소프트웨어학과", "17019013", 2, "재학", 1),
            Instant.parse("2026-05-29T08:10:00Z")),
        "요청 성공");
  }
}
