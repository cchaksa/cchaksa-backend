package com.chukchuk.haksa.domain.student.dto;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

//year, semester를 받아오기 위한 DTO
public class StudentSemesterDto {

    @Schema(description = "학생의 이수 학기 정보")
    public record StudentSemesterInfoResponse(
            @Schema(description = "이수 연도", example = "2023", required = true) int year,
            @Schema(description = "이수 학기 코드 (10: 1학기, 15: 여름학기, 20: 2학기, 25: 겨울학기)", example = "10", required = true) int semester
    ) {
        public static StudentSemesterInfoResponse from(SemesterSummaryResponse record) {
            return new StudentSemesterInfoResponse(
                    record.year(),
                    record.semester()
            );
        }
    }

}
