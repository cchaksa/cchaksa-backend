package com.chukchuk.haksa.domain.portal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class PortalLinkDto {

    @Schema(description = "포털 연동 job 생성 요청")
    public record LinkRequest(
            @NotBlank
            @JsonProperty("portal_type")
            @Schema(description = "포털 타입", example = "suwon")
            String portal_type,
            @NotBlank
            @Schema(description = "포털 아이디", example = "17019013")
            String username,
            @NotBlank
            @Schema(description = "포털 비밀번호", example = "pw")
            String password
    ) {}

    @Schema(description = "스크래핑 job 수락 응답")
    public record AcceptedResponse(
            @JsonProperty("job_id")
            @Schema(description = "job id", example = "job-123")
            String job_id,
            @Schema(description = "수락 상태", example = "accepted")
            String status,
            @JsonProperty("polling_endpoint")
            @Schema(description = "상태 조회 경로", example = "/portal/link/jobs/job-123")
            String polling_endpoint
    ) {}

    @Schema(description = "스크래핑 job 상태 응답")
    public record JobStatusResponse(
            @JsonProperty("job_id")
            String job_id,
            @JsonProperty("portal_type")
            String portal_type,
            String status,
            @JsonProperty("error_code")
            String error_code,
            @JsonProperty("error_message")
            String error_message,
            Boolean retryable,
            @JsonProperty("created_at")
            Instant created_at,
            @JsonProperty("updated_at")
            Instant updated_at,
            @JsonProperty("finished_at")
            Instant finished_at
    ) {}

    public record ScrapeResultCallbackRequest(
            @JsonProperty("job_id")
            String job_id,
            String status,
            @JsonProperty("result_payload")
            JsonNode result_payload,
            @JsonProperty("error_code")
            String error_code,
            @JsonProperty("error_message")
            String error_message,
            Boolean retryable,
            @JsonProperty("finished_at")
            Instant finished_at
    ) {}
}
