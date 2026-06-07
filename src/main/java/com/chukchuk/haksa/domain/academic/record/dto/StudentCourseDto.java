package com.chukchuk.haksa.domain.academic.record.dto;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

public class StudentCourseDto {

    // TODO: 타입 일치하는지 확인
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
                    description = "선교 영역 세부 코드 (LiberalArtsAreaCode). areaType 이 선교인 과목에 한해 노출되며, 그 외 영역에서는 응답에서 omit된다.",
                    example = "7",
                    nullable = true
            )
            @JsonInclude(JsonInclude.Include.NON_NULL)
            Integer liberalAreaCode,
            @Schema(description = "재수강 삭제 과목 여부") boolean isRetakeDelete
    ) {
        public static CourseDetailDto from(StudentCourse course) {
            return new CourseDetailDto(
                    String.valueOf(course.getId()),
                    course.getOffering().getCourse().getCourseName(),
                    course.getOffering().getCourse().getCourseCode(),
                    course.getOffering().getFacultyDivisionName(),
                    course.getOffering().getRawFacultyDivisionName(),
                    course.getOffering().getPoints(),
                    course.getOffering().getProfessor() != null ? course.getOffering().getProfessor().getProfessorName() : "미지정",
                    course.getGrade() != null ? course.getGrade().getValue().getValue() : "F",
                    course.getPoints() != null ? course.getPoints() : 0,
                    Objects.requireNonNullElse(course.getIsRetake(), false),
                    Objects.requireNonNullElse(course.getOffering().getIsVideoLecture(), false),
                    course.getOffering().getYear(),
                    course.getOffering().getSemester(),
                    course.getOriginalScore() != null ? course.getOriginalScore() : 0,
                    missionLiberalAreaCode(course),
                    course.isRetakeDeleted()
            );
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
