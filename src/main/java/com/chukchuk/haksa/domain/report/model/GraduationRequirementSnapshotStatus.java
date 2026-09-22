// 문의 생성 시점에 확인한 졸업요건 등록 상태를 정의한다.

package com.chukchuk.haksa.domain.report.model;

/** 문의 제출자의 졸업요건 데이터를 확인한 결과를 구분한다. */
public enum GraduationRequirementSnapshotStatus {
  AVAILABLE,
  NOT_AVAILABLE,
  UNKNOWN
}
