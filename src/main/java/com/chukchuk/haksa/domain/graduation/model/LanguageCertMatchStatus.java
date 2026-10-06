// 외국어 인증 기준 학과 매핑의 신뢰 상태를 정의하는 enum

package com.chukchuk.haksa.domain.graduation.model;

/** 학생의 외국어 인증 정보와 졸업 요건 간 일치 상태를 정의한다. */
public enum LanguageCertMatchStatus {
  VERIFIED,
  INFERRED,
  UNMAPPED
}
