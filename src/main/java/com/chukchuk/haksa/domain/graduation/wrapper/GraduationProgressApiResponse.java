package com.chukchuk.haksa.domain.graduation.wrapper;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;

/** 졸업요건 진행률 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "GraduationProgressApiResponse", description = "졸업 요건 진행 상황 응답")
public class GraduationProgressApiResponse extends SuccessResponse<GraduationProgressResponse> {

  /** 빈 영역 목록을 사용해 졸업 진행률 문서용 성공 응답을 생성한다. */
  public GraduationProgressApiResponse() {
    super(new GraduationProgressResponse(Collections.emptyList()), "요청 성공");
  }
}
