package com.chukchuk.haksa.domain.admin.report.dto;

import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import com.chukchuk.haksa.domain.report.model.GraduationRequirementSnapshotStatus;
import com.chukchuk.haksa.domain.report.model.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 관리자 문의 목록·상세·답변 API의 데이터 계약이다. */
public final class AdminReportDto {
  private AdminReportDto() {}

  /** 본문과 전체 스냅샷을 제외한 문의 목록 항목이다. */
  public record ListItem(
      UUID reportId,
      ReportStatus status,
      String title,
      UUID userId,
      String studentCode,
      Instant createdAt,
      Instant answeredAt) {}

  /** 관리자 문의 목록 페이지다. */
  public record PageResponse(
      List<ListItem> items,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean hasNext) {}

  /** 문의 생성 시점에 저장한 실제 사용자·학적 스냅샷이다. */
  public record SubmitterSnapshot(
      UUID submittedUserId,
      Long departmentId,
      String departmentName,
      String studentCode,
      Long primaryMajorId,
      String primaryMajorName,
      Long secondaryMajorId,
      String secondaryMajorName,
      Boolean transferStudent,
      Integer admissionYear,
      GraduationRequirementSnapshotStatus graduationRequirementStatus) {}

  /** 관리자 답변과 감사 식별 정보다. */
  public record AnswerInfo(
      String answer, Instant answeredAt, UUID adminAccountId, String adminDisplayName) {}

  /** 관리자 문의 상세 응답이다. */
  public record DetailResponse(
      UUID reportId,
      ReportStatus status,
      String title,
      String content,
      UUID userId,
      Instant createdAt,
      Instant updatedAt,
      SubmitterSnapshot submitter,
      AnswerInfo answer) {}

  /** 최초 답변 등록 요청이다. */
  public record AnswerRequest(
      @Schema(description = "답변 plain text", minLength = 1, maxLength = 5000)
          @NotBlank
          @Size(max = 5000)
          String answer) {
    /**
     * 답변 양끝 공백을 제거한다.
     *
     * @param answer 답변 plain text
     */
    public AnswerRequest {
      answer = answer == null ? null : answer.trim();
    }
  }

  /** 답변 저장 결과다. */
  public record AnswerResponse(
      UUID reportId,
      ReportStatus status,
      Instant answeredAt,
      UUID adminAccountId,
      String adminDisplayName,
      AdminRole adminRole) {}
}
