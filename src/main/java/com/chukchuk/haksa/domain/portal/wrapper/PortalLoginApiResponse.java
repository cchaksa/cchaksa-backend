// 포털 로그인 검증 성공 응답의 Swagger 문서용 래퍼
package com.chukchuk.haksa.domain.portal.wrapper;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PortalLoginApiResponse", description = "포털 로그인 검증 응답")
public class PortalLoginApiResponse extends SuccessResponse<PortalLinkDto.LoginResponse> {

    public PortalLoginApiResponse() {
        super(new PortalLinkDto.LoginResponse("portal-verification-token"), "요청 성공");
    }
}
