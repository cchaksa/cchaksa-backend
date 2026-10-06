package com.chukchuk.haksa.domain.academic.record.wrapper;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 학기 성적과 영역별 수강 과목 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "AcademicRecordApiResponse", description = "학기별 성적 및 수강 과목 정보 응답")
public class AcademicRecordApiResponse extends SuccessResponse<AcademicRecordResponse> {

  /** 빈 수강 과목 그룹을 담은 학기 성적 문서용 성공 응답을 생성한다. */
  public AcademicRecordApiResponse() {
    super(
        new AcademicRecordResponse(
            null, new AcademicRecordResponse.Courses(List.of(), List.of(), List.of())),
        "요청 성공");
  }
}
