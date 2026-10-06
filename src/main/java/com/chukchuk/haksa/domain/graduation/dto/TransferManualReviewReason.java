// 편입생 졸업진단에서 수동 확인이 필요한 사유를 표현한다.

package com.chukchuk.haksa.domain.graduation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 편입생 자동 졸업진단으로 확인할 수 없는 요건의 사유다. */
@Schema(description = "편입생 졸업진단 수동 확인 사유")
public enum TransferManualReviewReason {
  TRANSFER_ENTRY_GRADE_UNKNOWN,
  REGISTERED_SEMESTERS_NOT_VERIFIED,
  REQUIRED_COURSES_NOT_ASSESSABLE,
  ELECTIVE_RATIO_NOT_ASSESSABLE,
  MINOR_OR_LINKED_MAJOR_NOT_ASSESSABLE,
  GRADUATION_REVIEW_NOT_AVAILABLE,
  ACADEMIC_SUMMARY_INCOMPLETE,
  GRADUATION_REQUIREMENTS_NOT_FOUND,
  RECOGNIZED_CREDITS_INCOMPLETE,
  LANGUAGE_CERT_NOT_VERIFIED,
  DESIGNATED_COURSES_NOT_VERIFIED
}
