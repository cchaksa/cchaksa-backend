package com.chukchuk.haksa.application.academic.wrapper;

import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/** Springdoc에 포털 연동 성공 응답의 제네릭 payload 타입을 노출한다. */
@Schema(name = "ScrapingApiResponse", description = "포털 데이터 크롤링 응답")
public class ScrapingApiResponse extends SuccessResponse<ScrapingResponse> {

  /** Springdoc schema 생성을 위한 예시 스크래핑 응답으로 wrapper를 생성한다. */
  public ScrapingApiResponse() {
    super(ScrapingResponse.success("dummy-task-id", null), "요청 성공");
  }
}
