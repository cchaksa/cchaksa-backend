// 사용자 문의 API의 OpenAPI 계약을 정의한다.

package com.chukchuk.haksa.domain.report.controller.docs;

import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.wrapper.ReportCreateApiResponse;
import com.chukchuk.haksa.domain.report.wrapper.ReportDetailApiResponse;
import com.chukchuk.haksa.domain.report.wrapper.ReportListApiResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** 인증 사용자의 문의 생성과 본인 문의 목록·상세 조회 API 계약이다. */
@Tag(name = "Reports", description = "사용자 문의 API")
public interface ReportControllerDocs {

  /**
   * 문의를 생성한다.
   *
   * @param userDetails 인증 사용자 정보
   * @param request 문의 제목과 본문
   * @return 생성된 문의 정보
   */
  @Operation(
      summary = "문의 등록",
      description = "제목과 텍스트 본문으로 문의를 등록하고 생성 시점의 사용자·학적 정보를 보존합니다.",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "문의 등록 성공",
            content = @Content(schema = @Schema(implementation = ReportCreateApiResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "제목 또는 본문 유효성 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 정보 없음 (ErrorCode: U01)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<ReportDto.CreateResponse>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReportDto.CreateRequest request);

  /**
   * 본인 문의 목록을 최신순으로 조회한다.
   *
   * @param userDetails 인증 사용자 정보
   * @param page 0부터 시작하는 페이지
   * @param size 페이지 크기
   * @return 본인 문의 페이지
   */
  @Operation(
      summary = "내 문의 목록 조회",
      description = "로그인 사용자의 문의를 생성 시각과 식별자 역순으로 조회합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "문의 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ReportListApiResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "페이지 요청 범위 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<ReportDto.PageResponse>> getMyReports(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "0부터 시작하는 페이지") @RequestParam @Min(0) int page,
      @Parameter(description = "페이지 크기(1~100)") @RequestParam @Min(1) @Max(100) int size);

  /**
   * 본인 문의 상세를 조회한다.
   *
   * @param userDetails 인증 사용자 정보
   * @param reportId 문의 식별자
   * @return 문의 상세와 관리자 답변
   */
  @Operation(
      summary = "내 문의 상세 조회",
      description = "문의 제목·본문과 관리자 답변을 조회합니다. 다른 사용자의 문의에는 접근할 수 없습니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "문의 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = ReportDetailApiResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "403",
            description = "다른 사용자의 문의 (ErrorCode: C04)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "404",
            description = "문의 없음 (ErrorCode: R01)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<ReportDto.DetailResponse>> getDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "문의 식별자") @PathVariable UUID reportId);
}
