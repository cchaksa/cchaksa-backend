package com.chukchuk.haksa.domain.lectureevaluations.dto;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.chukchuk.haksa.domain.lectureevaluations.model.LectureEvaluationTag;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 강의평가 대상 조회와 제출·건너뛰기에 사용하는 요청·응답 형식을 묶는다. */
public class LectureEvaluationDto {

  /**
   * 강의평가 대상 학기와 평가할 성적 목록을 전달한다.
   *
   * @param evaluationStatus evaluation 상태
   * @param year 연도
   * @param semester 대상 학기
   * @param grades 평가 대상 수강 과목의 성적 카드 목록
   */
  public record RequiredResponse(
      @Schema(
              description = "강의평가 상태",
              allowableValues = {"NOT_RELEASED", "PENDING", "SKIPPED", "COMPLETED"},
              nullable = true)
          LectureEvaluationStatus evaluationStatus,
      @Schema(description = "강의평가 대상 연도") Integer year,
      @Schema(description = "강의평가 대상 학기 코드") Integer semester,
      @Schema(description = "성적 카드 목록") List<GradeCard> grades) {
    /**
     * 강의평가 대상 학기의 성적 없는 응답을 생성한다.
     *
     * @param evaluationStatus 강의평가 상태
     * @param year 연도
     * @param semester 학기
     * @return 성적 목록이 비어 있는 강의평가 응답
     */
    public static RequiredResponse withoutGrades(
        LectureEvaluationStatus evaluationStatus, Integer year, Integer semester) {
      return new RequiredResponse(evaluationStatus, year, semester, List.of());
    }
  }

  /**
   * 강의평가 대상 과목과 성적 정보를 표현한다.
   *
   * @param courseName 과목 이름
   * @param courseCode 과목 코드
   * @param courseId 과목 식별자
   * @param areaType 졸업 요건을 구분하는 영역
   * @param credits 학점
   * @param professor 교수
   * @param professorId 교수 식별자
   * @param grade 과목에서 취득한 성적
   * @param score 점수
   * @param liberalAreaCode 선교 과목에 연결된 교양 영역 코드이며 그 외에는 {@code null}
   */
  public record GradeCard(
      String courseName,
      String courseCode,
      Long courseId,
      FacultyDivision areaType,
      Integer credits,
      String professor,
      Long professorId,
      String grade,
      Integer score,
      @JsonInclude(JsonInclude.Include.NON_NULL) Integer liberalAreaCode) {
    /**
     * 수강 내역으로 강의평가용 성적 카드를 생성한다.
     *
     * @param studentCourse 수강 내역
     * @return 강의평가용 성적 카드
     */
    public static GradeCard from(StudentCourse studentCourse) {
      return new GradeCard(
          studentCourse.getOffering().getCourse().getCourseName(),
          studentCourse.getOffering().getCourse().getCourseCode(),
          studentCourse.getOffering().getCourse().getId(),
          studentCourse.getOffering().getFacultyDivisionName(),
          studentCourse.getOffering().getPoints(),
          studentCourse.getOffering().getProfessor() != null
              ? studentCourse.getOffering().getProfessor().getProfessorName()
              : "미지정",
          studentCourse.getOffering().getProfessor() != null
              ? studentCourse.getOffering().getProfessor().getId()
              : null,
          studentCourse.getGrade() != null && studentCourse.getGrade().getValue() != null
              ? studentCourse.getGrade().getValue().getValue()
              : null,
          studentCourse.getOriginalScore(),
          missionLiberalAreaCode(studentCourse));
    }

    private static Integer missionLiberalAreaCode(StudentCourse studentCourse) {
      if (studentCourse.getOffering().getFacultyDivisionName() != FacultyDivision.선교) {
        return null;
      }

      LiberalArtsAreaCode areaCode = studentCourse.getOffering().getLiberalArtsAreaCode();
      return areaCode != null ? areaCode.getCode() : null;
    }
  }

  /**
   * 한 학기의 강의평가 제출 항목을 전달한다.
   *
   * @param year 연도
   * @param semester 대상 학기
   * @param evaluations 과목·교수별 평가 내용 목록
   */
  public record SubmitRequest(
      @NotNull Integer year,
      @NotNull Integer semester,
      @Valid @NotNull @Size(min = 1) List<SubmitEvaluation> evaluations) {}

  /**
   * 한 과목과 교수에 대한 선택 태그와 후기를 전달한다.
   *
   * @param courseId 과목 식별자
   * @param professorId 교수 식별자
   * @param selectedTags 사용자가 선택한 강의평가 태그 목록
   * @param review 최대 2,000자의 선택 입력 후기
   */
  public record SubmitEvaluation(
      @NotNull Long courseId,
      @NotNull Long professorId,
      @NotNull List<LectureEvaluationTag> selectedTags,
      @Size(max = 2000) String review) {}

  /**
   * 강의평가를 건너뛸 대상 학기를 지정한다.
   *
   * @param year 연도
   * @param semester 대상 학기
   */
  public record SkipRequest(@NotNull Integer year, @NotNull Integer semester) {}
}
