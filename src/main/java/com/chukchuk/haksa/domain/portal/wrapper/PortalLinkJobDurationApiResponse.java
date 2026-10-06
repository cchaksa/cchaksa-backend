// 포털 링크 job 소요 시간 조회 성공 응답의 Swagger 문서용 래퍼

package com.chukchuk.haksa.domain.portal.wrapper;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 포털 연동 작업 소요 시간 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "PortalLinkJobDurationApiResponse", description = "포털 링크 job 소요 시간 조회 응답")
public class PortalLinkJobDurationApiResponse
    extends SuccessResponse<PortalLinkDto.JobDurationResponse> {

  /** 성공한 포털 연동 작업의 예시 소요 시간을 담은 문서용 응답을 생성한다. */
  public PortalLinkJobDurationApiResponse() {
    super(
        new PortalLinkDto.JobDurationResponse(
            "job-123",
            "succeeded",
            true,
            Instant.parse("2026-06-04T10:00:00Z"),
            Instant.parse("2026-06-04T10:00:12.345Z"),
            12_345L,
            "12s 345ms"),
        "요청 성공");
  }
}
