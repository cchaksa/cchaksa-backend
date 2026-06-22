package com.chukchuk.haksa.domain.lectureevaluations.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LectureEvaluationSubmitApiResponse", description = "강의평가 제출 응답")
public class LectureEvaluationSubmitApiResponse extends SuccessResponse<MessageOnlyResponse> {

    public LectureEvaluationSubmitApiResponse() {
        super(new MessageOnlyResponse("강의평가 저장 완료"), "요청 성공");
    }
}
