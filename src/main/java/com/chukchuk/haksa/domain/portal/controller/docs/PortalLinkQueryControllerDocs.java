package com.chukchuk.haksa.domain.portal.controller.docs;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Portal Link", description = "비동기 포털 연동 job 생성 및 폴링 안내")
public interface PortalLinkQueryControllerDocs {

    @Operation(
            summary = "비동기 job 상태 조회",
            description = "요청된 job_id의 진행 상태, 오류 코드, 완료 시점 등을 제공합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "상태 조회 성공",
                            content = @Content(schema = @Schema(implementation = PortalLinkDto.JobStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "job 미존재 또는 권한 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<PortalLinkDto.JobStatusResponse>> getJobStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String jobId
    );

    @Operation(
            summary = "비동기 job 요약 조회",
            description = "job이 성공적으로 완료된 경우 최신 학생 요약 데이터를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "요약 조회 성공",
                            content = @Content(schema = @Schema(implementation = PortalLinkDto.JobSummaryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "job 미존재 또는 권한 없음",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<PortalLinkDto.JobSummaryResponse>> getJobSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String jobId
    );
}
