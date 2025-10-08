package com.chukchuk.haksa.application.dto;

import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포털 연동 또는 재연동 및 학업 이력 동기화 성공 응답")
public record ScrapingResponse(
        @Schema(description = "작업 ID", example = "4aabf0d0-1c23-4f3d-845e-24c9c943deed")
        String taskId,
        @Schema(description = "학생 정보 요약")
        PortalConnectionResult.StudentInfo studentInfo,
        @Schema(description = "포털 연동 상태", example = "SUCCESS / ALREADY_CONNECTED")
        String status
) {
    public static ScrapingResponse success(String taskId, PortalConnectionResult.StudentInfo studentInfo) {
        return new ScrapingResponse(taskId, studentInfo, "SUCCESS");
    }

    public static ScrapingResponse alreadyConnected() {
        return new ScrapingResponse(null, null, "ALREADY_CONNECTED");
    }
}