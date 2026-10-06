// 편입생 지정과목의 원본 정보와 이수 상태를 전달한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 편입생 지정과목 한 건의 이수 현황이다. */
@Schema(description = "편입생 지정과목 이수 현황")
public record DesignatedCourseProgressDto(
    @Schema(description = "과목 코드", example = "C101") String courseCode,
    @Schema(description = "과목명", example = "자료구조") String courseName,
    @Schema(description = "지정과목 원본 학점", example = "3", nullable = true) Integer credits,
    @Schema(description = "실제 이수 상태") DesignatedCourseCompletionStatus status) {}
