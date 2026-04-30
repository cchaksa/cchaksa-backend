package com.chukchuk.haksa.domain.graduation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "이수 과목 정보")
public class CourseDto {
    @Schema(description = "이수 연도", example = "2023", required = true)
    private Integer year;
    @Schema(description = "과목명", example = "자료구조", required = true)
    private String courseName;
    @Schema(description = "학점", example = "3", required = true)
    private Integer credits;
    @Schema(description = "성적", example = "A+", required = true)
    private String grade;
    @Schema(description = "이수 학기", example = "10", required = true)
    private Integer semester;
    @Schema(
            description = "교양/선교 영역 세부 코드 (LiberalArtsAreaCode). 선교 영역 등 sub-area가 정의된 과목에 한해 노출되며, 그 외 영역에서는 응답에서 omit된다.",
            example = "7",
            nullable = true
    )
    private Integer liberalAreaCode;
}
