package com.chukchuk.haksa.domain.admin.report.controller;

import com.chukchuk.haksa.domain.admin.auth.security.AdminPrincipal;
import com.chukchuk.haksa.domain.admin.report.dto.AdminReportDto;
import com.chukchuk.haksa.domain.admin.report.dto.AdminReportSearchType;
import com.chukchuk.haksa.domain.admin.report.service.AdminReportService;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자와 CS 담당자에게 문의 조회와 최초 답변 API를 제공한다. */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Validated
public class AdminReportController {
  private final AdminReportService adminReportService;

  /**
   * 문의를 최신순으로 페이지 조회한다.
   *
   * @param page 0부터 시작하는 페이지
   * @param size 페이지 크기
   * @param status 선택 상태
   * @param searchType 정확 일치 검색 필드
   * @param query 검색 값
   * @return 문의 요약 페이지
   */
  @Operation(summary = "관리자 문의 목록 조회")
  @SecurityRequirement(name = "adminSession")
  @GetMapping
  public SuccessResponse<AdminReportDto.PageResponse> getReports(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) ReportStatus status,
      @RequestParam(required = false) AdminReportSearchType searchType,
      @RequestParam(required = false) String query) {
    return SuccessResponse.of(adminReportService.getReports(page, size, status, searchType, query));
  }

  /**
   * 문의 본문과 제출자 스냅샷을 상세 조회한다.
   *
   * @param reportId 문의 UUID
   * @return 문의 상세
   */
  @Operation(summary = "관리자 문의 상세 조회")
  @SecurityRequirement(name = "adminSession")
  @GetMapping("/{reportId}")
  public SuccessResponse<AdminReportDto.DetailResponse> getDetail(@PathVariable UUID reportId) {
    return SuccessResponse.of(adminReportService.getDetail(reportId));
  }

  /**
   * 답변 대기 문의에 최초 답변을 저장한다.
   *
   * @param reportId 문의 UUID
   * @param request 답변 plain text
   * @param principal 인증 관리자
   * @return 저장된 답변 감사 정보
   */
  @Operation(summary = "관리자 문의 최초 답변 등록")
  @SecurityRequirement(name = "adminSession")
  @Parameter(
      name = "X-XSRF-TOKEN",
      in = ParameterIn.HEADER,
      required = true,
      description = "관리자 CSRF 쿠키 값")
  @PostMapping("/{reportId}/answer")
  public SuccessResponse<AdminReportDto.AnswerResponse> answer(
      @PathVariable UUID reportId,
      @Valid @RequestBody AdminReportDto.AnswerRequest request,
      @AuthenticationPrincipal AdminPrincipal principal) {
    return SuccessResponse.of(adminReportService.answer(reportId, request.answer(), principal));
  }
}
