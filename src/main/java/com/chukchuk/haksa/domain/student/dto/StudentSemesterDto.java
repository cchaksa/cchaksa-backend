package com.chukchuk.haksa.domain.student.dto;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 학생이 이수한 연도·학기 목록과 현재 학기 정보를 전달한다. */
public class StudentSemesterDto {

  /**
   * 학생이 이수한 연도와 학기 코드를 표현한다.
   *
   * @param year 연도
   * @param semester 대상 학기
   */
  @Schema(description = "학생의 이수 학기 정보")
  public record StudentSemesterInfoResponse(
      @Schema(description = "이수 연도", example = "2023", required = true) int year,
      @Schema(
              description = "이수 학기 코드 (10: 1학기, 15: 여름학기, 20: 2학기, 25: 겨울학기)",
              example = "10",
              required = true)
          int semester) {
    /**
     * 학기 요약을 학생 학기 응답으로 변환한다.
     *
     * @param record 연결할 학사 기록
     * @return 학기 요약에서 변환한 이수 학기 정보
     */
    public static StudentSemesterInfoResponse from(SemesterSummaryResponse record) {
      return new StudentSemesterInfoResponse(record.year(), record.semester());
    }
  }
}
