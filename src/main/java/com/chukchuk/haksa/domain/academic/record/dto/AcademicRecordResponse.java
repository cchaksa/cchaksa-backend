package com.chukchuk.haksa.domain.academic.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 한 학기의 성적과 전공·교양·기타 영역별 수강 과목을 묶는다.
 *
 * @param semesterGrade 조회한 학기의 성적 요약
 * @param summary 저장하거나 비교할 학사 요약
 */
@Schema(description = "학기별 성적 및 수강 과목 응답")
public record AcademicRecordResponse(
    @Schema(description = "학기 성적 정보", required = true)
        SemesterAcademicRecordDto.SemesterGradeResponse semesterGrade,
    @Schema(description = "수강 과목 목록", required = true) Courses courses
    //        @Schema(description = "학업 요약 정보") Summary summary
    ) {

  /**
   * 수강 과목을 전공·교양·기타 영역으로 구분해 전달한다.
   *
   * @param major 주전공
   * @param liberal 교양 영역 수강 과목 목록
   * @param etc 전공과 교양 외 영역의 수강 과목 목록
   */
  @Schema(description = "수강 과목 목록")
  public record Courses(
      @Schema(description = "전공 과목 목록", required = true)
          List<StudentCourseDto.CourseDetailDto> major,
      @Schema(description = "교양 과목 목록", required = true)
          List<StudentCourseDto.CourseDetailDto> liberal,
      @Schema(description = "기타 과목 목록", required = true)
          List<StudentCourseDto.CourseDetailDto> etc) {}
}
