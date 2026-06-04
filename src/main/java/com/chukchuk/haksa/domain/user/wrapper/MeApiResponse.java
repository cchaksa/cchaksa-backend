// 내 사용자 정보 조회 API의 문서용 응답 래퍼
package com.chukchuk.haksa.domain.user.wrapper;

import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MeApiResponse", description = "내 사용자 정보 조회 응답")
public class MeApiResponse extends SuccessResponse<UserDto.MeResponse> {

    public MeApiResponse() {
        super(new UserDto.MeResponse(true), "요청 성공");
    }
}
