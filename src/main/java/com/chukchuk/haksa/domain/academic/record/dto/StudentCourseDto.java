package com.chukchuk.haksa.domain.academic.record.dto;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/** 학생 수강 내역을 API 과목 상세 응답으로 변환한다. */
public class StudentCourseDto {

  /**
   * 학생의 수강 과목과 성적 상세 정보를 표현한다.
   *
   * @param id 학생 수강 기록 식별자
   * @param courseName 과목 이름
   * @param courseCode 과목 코드
   * @param areaType 졸업 요건을 구분하는 영역
   * @param rawAreaType 포털에서 받은 원본 이수 구분
   * @param credits 학점
   * @param professor 교수
   * @param grade 과목에서 취득한 성적
   * @param score 점수
   * @param isRetake 재수강 과목인지 여부
   * @param isOnline 온라인 강의인지 여부
   * @param year 연도
   * @param semester 대상 학기
   * @param originalScore 원점수
   * @param liberalAreaCode 선교 과목의 교양 영역 코드이며 그 외에는 {@code null}
   * @param isRetakeDelete 재수강으로 대체돼 성적 계산에서 제외되는지 여부
   */
  @Schema(description = "수강 과목 상세 정보")
  public record CourseDetailDto(
      @Schema(description = "수강 ID") String id,
      @Schema(description = "과목명") String courseName,
      @Schema(description = "학수번호") String courseCode,
      @Schema(description = "영역 (전공/교양 등)") FacultyDivision areaType,
      @Schema(description = "포털 원본 이수 구분", nullable = true) String rawAreaType,
      @Schema(description = "학점") Integer credits,
      @Schema(description = "교수명") String professor,
      @Schema(description = "성적") String grade,
      @Schema(description = "실 점수") Integer score,
      @Schema(description = "재수강 여부") Boolean isRetake,
      @Schema(description = "사이버 강의 여부") Boolean isOnline,
      @Schema(description = "이수 연도") Integer year,
      @Schema(description = "이수 학기") Integer semester,
      @Schema(description = "원점수") Integer originalScore,
      @Schema(
              description =
                  "선교 영역 세부 코드 (LiberalArtsAreaCode). areaType 이 선교인 과목에 한해 노출되며, 그 외 영역에서는 "
                      + "응답에서 omit된다.",
              example = "7",
              nullable = true)
          @JsonInclude(JsonInclude.Include.NON_NULL)
          Integer liberalAreaCode,
      @Schema(description = "재수강 삭제 과목 여부") boolean isRetakeDelete) {
    /**
     * 학생 수강 내역을 과목 상세 응답으로 변환한다.
     *
     * @param course 학생 수강 내역
     * @return 과목 상세 응답
     */
    public static CourseDetailDto from(StudentCourse course) {
      return new CourseDetailDto(
          String.valueOf(course.getId()),
          course.getOffering().getCourse().getCourseName(),
          course.getOffering().getCourse().getCourseCode(),
          course.getOffering().getFacultyDivisionName(),
          course.getOffering().getRawFacultyDivisionName(),
          course.getPoints() != null ? course.getPoints() : 0,
          course.getOffering().getProfessor() != null
              ? course.getOffering().getProfessor().getProfessorName()
              : "미지정",
          course.getGrade() != null ? course.getGrade().getValue().getValue() : "F",
          course.getPoints() != null ? course.getPoints() : 0,
          Objects.requireNonNullElse(course.getIsRetake(), false),
          Objects.requireNonNullElse(course.getOffering().getIsVideoLecture(), false),
          course.getOffering().getYear(),
          course.getOffering().getSemester(),
          course.getOriginalScore() != null ? course.getOriginalScore() : 0,
          missionLiberalAreaCode(course),
          course.isRetakeDeleted());
    }

    private static Integer missionLiberalAreaCode(StudentCourse course) {
      if (course.getOffering().getFacultyDivisionName() != FacultyDivision.선교) {
        return null;
      }

      LiberalArtsAreaCode areaCode = course.getOffering().getLiberalArtsAreaCode();
      return areaCode != null ? areaCode.getCode() : null;
    }
  }
}
