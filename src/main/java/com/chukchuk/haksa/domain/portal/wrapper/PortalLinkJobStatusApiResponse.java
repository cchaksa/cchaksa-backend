// 포털 링크 job 상태 조회 성공 응답의 Swagger 문서용 래퍼
package com.chukchuk.haksa.domain.portal.wrapper;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "PortalLinkJobStatusApiResponse", description = "포털 링크 job 상태 조회 응답")
public class PortalLinkJobStatusApiResponse extends SuccessResponse<PortalLinkDto.JobStatusResponse> {

    public PortalLinkJobStatusApiResponse() {
        super(new PortalLinkDto.JobStatusResponse(
                "job-123",
                "suwon",
                "queued",
                null,
                null,
                null,
                Instant.parse("2026-05-29T08:00:00Z"),
                Instant.parse("2026-05-29T08:00:00Z"),
                null
        ), "요청 성공");
    }
}
