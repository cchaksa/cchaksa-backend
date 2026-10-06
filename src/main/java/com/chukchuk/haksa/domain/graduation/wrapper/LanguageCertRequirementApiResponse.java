// 외국어 인증 기준 조회 성공 응답의 OpenAPI wrapper

package com.chukchuk.haksa.domain.graduation.wrapper;

import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** 외국어 인증 기준 조회 성공 응답의 OpenAPI 예시를 제공한다. */
@Schema(name = "LanguageCertRequirementApiResponse", description = "외국어 인증 기준 조회 응답")
public class LanguageCertRequirementApiResponse
    extends SuccessResponse<LanguageCertRequirementResponse> {

  /** 외국어 인증 기준 조회 성공 응답의 예시 값과 메시지를 구성한다. */
  public LanguageCertRequirementApiResponse() {
    super(
        LanguageCertRequirementResponse.unmapped("2000763", "자유전공학부", 2025, "기준표에 직접 행이 없음"),
        "요청 성공");
  }
}
