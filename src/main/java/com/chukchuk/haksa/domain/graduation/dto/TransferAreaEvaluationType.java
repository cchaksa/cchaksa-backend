// 편입생 영역별 졸업요건 평가 방식을 표현한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 편입생 영역이 기준 비교 대상인지 여부를 구분한다. */
@Schema(description = "편입생 영역별 평가 방식")
public enum TransferAreaEvaluationType {
  /** 필요학점과 충족 여부를 계산할 수 있는 영역이다. */
  COMPARISON,

  /** 필요학점을 비교하지 않고 취득학점만 제공하는 영역이다. */
  EARNED_ONLY,

  /** 기준 데이터가 없어 비교할 수 없는 영역이다. */
  UNAVAILABLE
}
