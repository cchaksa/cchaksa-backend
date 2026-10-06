package com.chukchuk.haksa.domain.portal.controller.docs;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.portal.wrapper.PortalLinkAcceptedApiResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/** 포털 자격 증명으로 비동기 연동 작업을 생성하는 API를 정의한다. */
@Tag(name = "Portal Link", description = "비동기 포털 연동 job 생성 및 폴링 안내")
public interface PortalLinkCommandControllerDocs {

  /**
   * 포털 자격 증명으로 비동기 포털 연동 작업을 생성한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @param idempotencyKey 멱등성 키
   * @param request 포털 유형·아이디·비밀번호를 포함한 작업 생성 요청
   * @return 접수된 작업 식별자와 상태 조회 경로
   */
  @Operation(
      summary = "포털 연동 job 생성",
      description =
          "포털 자격 증명으로 비동기 스크래핑 job을 생성하고 polling endpoint를 반환합니다. 포털 로그인 실패는 job 상태 조회로 확인합니다.",
      responses = {
        @ApiResponse(
            responseCode = "202",
            description = "요청 수락",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PortalLinkAcceptedApiResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 입력 (INVALID_ARGUMENT)",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "401",
            description = "서비스 접근 토큰 인증 실패",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "409",
            description = "중복 요청 (SCRAPE_JOB_DUPLICATED)",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<PortalLinkDto.AcceptedResponse>> createPortalLinkJob(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestHeader("Idempotency-Key")
          @Parameter(
              name = "Idempotency-Key",
              description = "동일 요청 deduplication 용 키",
              in = ParameterIn.HEADER,
              required = true)
          String idempotencyKey,
      @Valid @RequestBody PortalLinkDto.LinkRequest request);
}
