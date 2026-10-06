package com.chukchuk.haksa.domain.academic.record.dto;

import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** 학생의 누적 학사 기록을 API 요약 응답으로 변환한다. */
public class StudentAcademicRecordDto {

  /**
   * 누적 학점·평점·백분위와 졸업 필요 학점 요약을 담는다.
   *
   * @param totalEarnedCredits total 취득 학점
   * @param cumulativeGpa 누적 평점
   * @param percentile 누적 성적의 백분위
   * @param requiredCredits required 학점
   */
  @Schema(description = "학업 성적 요약 정보")
  public record AcademicSummaryResponse(
      @Schema(description = "총 취득 학점", example = "120", required = true) Integer totalEarnedCredits,
      @Schema(description = "누적 GPA", example = "3.76", required = true) BigDecimal cumulativeGpa,
      @Schema(description = "전체 백분위", example = "87.5", required = true) BigDecimal percentile,
      @Schema(description = "필요 졸업 학점", example = "130", required = true) Integer requiredCredits) {
    /**
     * 학생 학사 기록을 졸업 학점이 포함된 요약 응답으로 변환한다.
     *
     * @param studentAcademicRecord 학생 학사 기록
     * @param requiredCredits 졸업에 필요한 총 학점
     * @return 학생 학사 요약 응답
     */
    public static AcademicSummaryResponse from(
        StudentAcademicRecord studentAcademicRecord, Integer requiredCredits) {
      return new AcademicSummaryResponse(
          studentAcademicRecord.getTotalEarnedCredits(),
          studentAcademicRecord.getCumulativeGpa(),
          studentAcademicRecord.getPercentile(),
          requiredCredits);
    }
  }
}
