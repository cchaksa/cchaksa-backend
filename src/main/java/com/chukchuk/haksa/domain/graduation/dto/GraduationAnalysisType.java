// 졸업진단 대상 학생 유형을 표현한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 졸업진단이 적용된 학생 유형이다. */
@Schema(description = "졸업진단 학생 유형")
public enum GraduationAnalysisType {
  REGULAR,
  TRANSFER
}
