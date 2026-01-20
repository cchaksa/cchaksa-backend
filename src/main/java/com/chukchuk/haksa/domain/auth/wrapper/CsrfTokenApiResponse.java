package com.chukchuk.haksa.domain.auth.wrapper;

import com.chukchuk.haksa.domain.auth.dto.AuthDto.CsrfTokenResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CsrfTokenApiResponse", description = "CSRF 토큰 응답 포맷")
public class CsrfTokenApiResponse extends SuccessResponse<CsrfTokenResponse> {

    public CsrfTokenApiResponse() {
        super(new CsrfTokenResponse("csrf-token-value", "X-XSRF-TOKEN"), "요청 성공");
    }
}
