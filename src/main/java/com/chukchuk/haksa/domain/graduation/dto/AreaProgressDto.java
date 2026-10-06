package com.chukchuk.haksa.domain.graduation.dto;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 졸업 요건 영역별 기준과 학생의 이수 진행도를 전달한다. */
@Getter
@AllArgsConstructor
@Schema(description = "졸업 요건 영역별 이수 현황")
public class AreaProgressDto {
  @Schema(description = "영역 유형 (예: 전핵, 전선, 선교 등)", required = true)
  private FacultyDivision areaType;

  @Schema(description = "해당 영역에서 필요한 학점", example = "60", required = true)
  private Integer requiredCredits;

  @Schema(description = "학생이 이수한 학점", example = "45", required = true)
  private Integer earnedCredits;

  @Schema(description = "필수 선택 과목 수", example = "2", required = true)
  private Integer requiredElectiveCourses;

  @Schema(description = "학생이 이수한 선택 과목 수", example = "1", required = true)
  private Integer completedElectiveCourses;

  @Schema(description = "총 선택 과목 수", example = "3", required = true)
  private Integer totalElectiveCourses;

  @Schema(description = "학생이 이수한 과목 목록", required = true)
  private List<CourseDto> courses;
}
