package com.chukchuk.haksa.domain.lectureevaluations.dto;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class LectureEvaluationDto {

    public record RequiredResponse(
            @Schema(description = "강의평가 상태", allowableValues = {"PENDING", "SKIPPED", "COMPLETED"}, nullable = true)
            LectureEvaluationStatus evaluationStatus,
            @Schema(description = "강의평가 대상 연도") Integer year,
            @Schema(description = "강의평가 대상 학기 코드") Integer semester,
            @Schema(description = "성적 카드 목록") List<GradeCard> grades
    ) {
        public static RequiredResponse withoutGrades(LectureEvaluationStatus evaluationStatus, Integer year, Integer semester) {
            return new RequiredResponse(evaluationStatus, year, semester, List.of());
        }
    }

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
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Integer liberalAreaCode
    ) {
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
                    studentCourse.getGrade() != null ? studentCourse.getGrade().getValue().getValue() : null,
                    studentCourse.getOriginalScore(),
                    missionLiberalAreaCode(studentCourse)
            );
        }

        private static Integer missionLiberalAreaCode(StudentCourse studentCourse) {
            if (studentCourse.getOffering().getFacultyDivisionName() != FacultyDivision.선교) {
                return null;
            }

            LiberalArtsAreaCode areaCode = studentCourse.getOffering().getLiberalArtsAreaCode();
            return areaCode != null ? areaCode.getCode() : null;
        }
    }

    public record SubmitRequest(
            @NotNull Integer year,
            @NotNull Integer semester,
            @Valid
            @NotNull
            @Size(min = 1)
            List<SubmitEvaluation> evaluations
    ) {}

    public record SubmitEvaluation(
            @NotNull Long courseId,
            @NotNull Long professorId,
            @NotNull List<String> selectedTags,
            @Size(max = 2000) String review
    ) {}

    public record SkipRequest(
            @NotNull Integer year,
            @NotNull Integer semester
    ) {}
}
