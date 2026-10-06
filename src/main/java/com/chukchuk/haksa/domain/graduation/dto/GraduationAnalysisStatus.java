// 졸업진단 자동 계산 상태를 표현한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 졸업진단 결과의 자동 판정 상태다. */
@Schema(description = "졸업진단 분석 상태")
public enum GraduationAnalysisStatus {
  CALCULATED,
  MANUAL_REVIEW_REQUIRED
}
