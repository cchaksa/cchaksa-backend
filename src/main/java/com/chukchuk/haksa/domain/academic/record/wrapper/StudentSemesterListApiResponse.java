package com.chukchuk.haksa.domain.academic.record.wrapper;

import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;

/** 이수 학기 목록 조회 성공 응답의 OpenAPI 스키마 예시를 제공한다. */
@Schema(name = "StudentSemesterListApiResponse", description = "사용자 학기 목록 응답")
public class StudentSemesterListApiResponse
    extends SuccessResponse<List<StudentSemesterDto.StudentSemesterInfoResponse>> {
  /** 빈 이수 학기 목록을 담은 문서용 성공 응답을 생성한다. */
  public StudentSemesterListApiResponse() {
    super(Collections.emptyList(), "요청 성공");
  }
}
