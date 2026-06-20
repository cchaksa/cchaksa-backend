package com.chukchuk.haksa.domain.lectureevaluations.wrapper;

import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LectureEvaluationRequiredApiResponse", description = "강의평가 필요 여부 응답")
public class LectureEvaluationRequiredApiResponse extends SuccessResponse<LectureEvaluationDto.RequiredResponse> {

    public LectureEvaluationRequiredApiResponse() {
        super(new LectureEvaluationDto.RequiredResponse(false, 2026, 10, List.of()), "요청 성공");
    }
}
