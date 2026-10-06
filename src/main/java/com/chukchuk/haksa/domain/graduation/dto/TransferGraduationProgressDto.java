// 편입생 졸업요건의 자동 계산 결과와 수동 확인 사유를 전달한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/** 편입생의 졸업요건 진행 현황과 영역별 이수 결과를 제공한다. */
@Schema(description = "편입생 졸업요건 부분 진단 결과")
public record TransferGraduationProgressDto(
    @Schema(description = "졸업 필요 총학점", example = "130") int requiredTotalCredits,
    @Schema(description = "포털 누적 취득학점", example = "112", nullable = true)
        Integer totalEarnedCredits,
    @Schema(description = "졸업까지 남은 학점", example = "18", nullable = true) Integer remainingCredits,
    @Schema(description = "총 취득학점 충족 여부", nullable = true) Boolean creditsFulfilled,
    @Schema(description = "편입 인정학점", example = "65", nullable = true)
        Integer recognizedTransferCredits,
    @Schema(description = "누적 GPA", example = "3.2", nullable = true) BigDecimal cumulativeGpa,
    @Schema(description = "적용 최소 GPA", example = "2.0") BigDecimal requiredGpa,
    @Schema(description = "GPA 충족 여부", nullable = true) Boolean gpaFulfilled,
    @Schema(description = "저장된 이수 학기 수", example = "3", nullable = true) Integer completedSemesters,
    @Schema(description = "지정과목 스냅샷 새로고침 필요 여부") boolean designatedCoursesNeedsRefresh,
    @Schema(description = "지정과목 이수 현황") List<DesignatedCourseProgressDto> designatedCourses,
    @Schema(description = "자동 판정할 수 없는 요건 존재 여부") boolean manualReviewRequired,
    @Schema(description = "수동 확인이 필요한 요건 목록") List<TransferManualReviewReason> manualReviewReasons,
    @Schema(description = "편입생 영역별 이수 현황") List<TransferAreaProgressDto> areas,
    @Schema(description = "실제 이수한 지정과목의 취득학점", nullable = true) Integer designatedEarnedCredits,
    @Schema(description = "지정과목 취득학점을 계산할 수 없는 사유")
        List<String> designatedCreditUnavailableReasons) {

  /** 기존 부분 진단 생성 계약을 유지하는 편입생 결과 생성자다. */
  public TransferGraduationProgressDto(
      int requiredTotalCredits,
      Integer totalEarnedCredits,
      Integer remainingCredits,
      Boolean creditsFulfilled,
      Integer recognizedTransferCredits,
      BigDecimal cumulativeGpa,
      BigDecimal requiredGpa,
      Boolean gpaFulfilled,
      Integer completedSemesters,
      boolean designatedCoursesNeedsRefresh,
      List<DesignatedCourseProgressDto> designatedCourses,
      boolean manualReviewRequired,
      List<TransferManualReviewReason> manualReviewReasons) {
    this(
        requiredTotalCredits,
        totalEarnedCredits,
        remainingCredits,
        creditsFulfilled,
        recognizedTransferCredits,
        cumulativeGpa,
        requiredGpa,
        gpaFulfilled,
        completedSemesters,
        designatedCoursesNeedsRefresh,
        designatedCourses,
        manualReviewRequired,
        manualReviewReasons,
        List.of(),
        null,
        List.of());
  }

  /** 목록 필드를 방어적 복사해 편입생 진단 결과를 생성한다. */
  public TransferGraduationProgressDto {
    designatedCourses = List.copyOf(designatedCourses);
    manualReviewReasons = List.copyOf(manualReviewReasons);
    areas = areas == null ? List.of() : List.copyOf(areas);
    designatedCreditUnavailableReasons =
        designatedCreditUnavailableReasons == null
            ? List.of()
            : List.copyOf(designatedCreditUnavailableReasons);
  }
}
