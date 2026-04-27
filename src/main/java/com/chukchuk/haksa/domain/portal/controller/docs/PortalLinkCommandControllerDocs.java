package com.chukchuk.haksa.domain.portal.controller.docs;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Portal Link", description = "비동기 포털 연동 job 생성 및 폴링 안내")
public interface PortalLinkCommandControllerDocs {

    @Operation(
            summary = "포털 연동 job 생성",
            description = "포털 자격 증명을 받아 비동기 스크래핑 job을 생성하고 polling endpoint를 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "요청 수락",
                            content = @Content(schema = @Schema(implementation = PortalLinkDto.AcceptedResponse.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 입력 (INVALID_ARGUMENT)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "401", description = "자격 증명 오류 (PORTAL_LOGIN_FAILED)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "409", description = "중복 요청 (SCRAPE_JOB_DUPLICATED)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<PortalLinkDto.AcceptedResponse>> createPortalLinkJob(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Idempotency-Key")
            @Parameter(name = "Idempotency-Key", description = "동일 요청 deduplication 용 키", in = ParameterIn.HEADER, required = true)
            String idempotencyKey,
            @Valid @RequestBody PortalLinkDto.LinkRequest request
    );
}
