package com.chukchuk.haksa.domain.lectureevaluations.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LectureEvaluationSkipApiResponse", description = "강의평가 건너뛰기 응답")
public class LectureEvaluationSkipApiResponse extends SuccessResponse<MessageOnlyResponse> {

    public LectureEvaluationSkipApiResponse() {
        super(new MessageOnlyResponse("강의평가 건너뛰기 완료"), "요청 성공");
    }
}
