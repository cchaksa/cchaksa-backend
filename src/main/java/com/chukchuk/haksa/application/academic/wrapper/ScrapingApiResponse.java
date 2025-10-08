package com.chukchuk.haksa.application.academic.wrapper;

import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ScrapingApiResponse", description = "포털 데이터 크롤링 응답")
public class ScrapingApiResponse extends SuccessResponse<ScrapingResponse> {

    public ScrapingApiResponse() {
        super(ScrapingResponse.success("dummy-task-id", null), "요청 성공");
    }
}
