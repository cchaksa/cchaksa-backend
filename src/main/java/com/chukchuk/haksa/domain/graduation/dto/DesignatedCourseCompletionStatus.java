// 지정과목의 이수 판정 상태를 표현한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 지정과목과 실제 이수 기록을 비교한 결과다. */
@Schema(description = "지정과목 이수 상태")
public enum DesignatedCourseCompletionStatus {
  COMPLETED,
  NOT_COMPLETED,
  UNKNOWN
}
