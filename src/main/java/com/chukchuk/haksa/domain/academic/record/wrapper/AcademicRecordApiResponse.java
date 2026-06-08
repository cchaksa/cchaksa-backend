package com.chukchuk.haksa.domain.academic.record.wrapper;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "AcademicRecordApiResponse", description = "학기별 성적 및 수강 과목 정보 응답")
public class AcademicRecordApiResponse extends SuccessResponse<AcademicRecordResponse> {

    public AcademicRecordApiResponse() {
        super(
                new AcademicRecordResponse(null, new AcademicRecordResponse.Courses(List.of(), List.of(), List.of())),
                "요청 성공"
        );
    }
}
