// 사용자 문의 생성과 본인 문의 조회 HTTP 요청을 처리한다.

package com.chukchuk.haksa.domain.report.controller;

import com.chukchuk.haksa.domain.report.controller.docs.ReportControllerDocs;
import com.chukchuk.haksa.domain.report.dto.ReportDto;
import com.chukchuk.haksa.domain.report.service.ReportService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 인증 사용자의 문의 생성·목록·상세 요청을 문의 서비스에 위임한다. */
@Validated
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

  private final ReportService reportService;

  @Override
  @PostMapping
  public ResponseEntity<SuccessResponse<ReportDto.CreateResponse>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody ReportDto.CreateRequest request) {
    ReportDto.CreateResponse response = reportService.create(userDetails.getId(), request);
    return ResponseEntity.created(URI.create("/api/reports/" + response.id()))
        .body(SuccessResponse.of(response));
  }

  @Override
  @GetMapping
  public ResponseEntity<SuccessResponse<ReportDto.PageResponse>> getMyReports(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(
        SuccessResponse.of(reportService.getMyReports(userDetails.getId(), page, size)));
  }

  @Override
  @GetMapping("/{reportId}")
  public ResponseEntity<SuccessResponse<ReportDto.DetailResponse>> getDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID reportId) {
    return ResponseEntity.ok(
        SuccessResponse.of(reportService.getDetail(userDetails.getId(), reportId)));
  }
}
