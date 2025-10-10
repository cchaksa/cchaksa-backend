package com.chukchuk.haksa.domain.academic.record.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "학기별 성적 및 수강 과목 응답")
public record AcademicRecordResponse(
        @Schema(description = "학기 성적 정보", required = true) SemesterAcademicRecordDto.SemesterGradeResponse semesterGrade,
        @Schema(description = "수강 과목 목록", required = true) Courses courses
//        @Schema(description = "학업 요약 정보") Summary summary
) {

    @Schema(description = "수강 과목 목록")
    public record Courses(
            @Schema(description = "전공 과목 목록", required = true) List<StudentCourseDto.CourseDetailDto> major,
            @Schema(description = "교양 과목 목록", required = true) List<StudentCourseDto.CourseDetailDto> liberal
    ) {}
}