package com.chukchuk.haksa.domain.portal.controller.docs;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Portal Link", description = "비동기 포털 연동 job 생성 및 폴링 안내 | [Internal] Callback")
public interface PortalLinkCallbackControllerDocs {

    @Operation(
            summary = "[Internal] 스크래핑 결과 콜백 수신",
            description = "스크래핑 워커가 API Gateway를 통해 전달하는 job 상태와 result_s3_key를 검증하고 DB에 반영합니다.\n"
                    + "실제 결과 payload는 result_s3_key 기반으로 백엔드가 S3에서 조회합니다.\n"
                    + "HMAC 서명 규칙: signature = HMAC_SHA256(\"{timestamp}.{rawBody}\").",
            responses = {
                    @ApiResponse(responseCode = "200", description = "콜백 처리 완료",
                            content = @Content(schema = @Schema(implementation = MessageOnlyResponse.class))),
                    @ApiResponse(responseCode = "400", description = "서명 검증 실패 (INVALID_CALLBACK_SIGNATURE)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 콜백 요청 (SCRAPE_INVALID_CALLBACK_REQUEST)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "404", description = "job 미존재 (PORTAL_JOB_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> handleCallback(
            @RequestBody String rawBody,
            @RequestHeader("X-Timestamp")
            @Parameter(name = "X-Timestamp", description = "epoch millis (UTC)", in = ParameterIn.HEADER, required = true)
            String timestamp,
            @RequestHeader("X-Signature")
            @Parameter(name = "X-Signature", description = "HMAC-SHA256 서명 값", in = ParameterIn.HEADER, required = true)
            String signature
    );
}
