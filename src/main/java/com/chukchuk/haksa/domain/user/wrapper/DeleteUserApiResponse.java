package com.chukchuk.haksa.domain.user.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 회원 탈퇴 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "DeleteUserApiResponse", description = "회원 탈퇴 응답 포맷")
public class DeleteUserApiResponse extends SuccessResponse<MessageOnlyResponse> {

  /** 회원 탈퇴 성공 응답의 예시 값과 메시지를 구성한다. */
  public DeleteUserApiResponse() {
    super(new MessageOnlyResponse("회원 탈퇴가 완료되었습니다."), "요청 성공");
  }
}
