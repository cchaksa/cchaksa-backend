// 내부 스크래핑 콜백 성공 응답의 Swagger 문서용 래퍼

package com.chukchuk.haksa.domain.portal.wrapper;

import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 스크래핑 결과 콜백 처리 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "PortalLinkCallbackApiResponse", description = "포털 링크 내부 콜백 처리 응답")
public class PortalLinkCallbackApiResponse extends SuccessResponse<MessageOnlyResponse> {

  /** 콜백 처리 완료 메시지를 담은 문서용 성공 응답을 생성한다. */
  public PortalLinkCallbackApiResponse() {
    super(new MessageOnlyResponse("콜백 처리 완료"), "요청 성공");
  }
}
