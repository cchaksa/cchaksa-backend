// 졸업요건 조회 실패에 필요한 Sentry MDC 문맥을 설정하는 유틸리티

package com.chukchuk.haksa.domain.graduation.policy;

import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.global.logging.util.HashUtil;
import org.slf4j.MDC;

/** 졸업요건 조회 실패를 추적할 학생·전공 정보를 MDC에 등록한다. */
public final class GraduationMdcContext {

  private GraduationMdcContext() {}

  /**
   * 학생과 전공 정보를 원문 학번 노출 없이 현재 MDC에 등록한다.
   *
   * @param student 조회 대상 학생
   * @param primaryMajorId 주전공 식별자
   * @param secondaryMajorId 복수전공 식별자, 없으면 {@code null}
   * @param admissionYear 입학연도
   */
  public static void bind(
      Student student, Long primaryMajorId, Long secondaryMajorId, int admissionYear) {
    MDC.put("studentCodeHash", HashUtil.sha256Short(student.getStudentCode()));
    MDC.put("admissionYear", String.valueOf(admissionYear));
    MDC.put("departmentId", String.valueOf(primaryMajorId));
    MDC.put(
        "secondaryDepartmentId",
        secondaryMajorId == null ? "NONE" : String.valueOf(secondaryMajorId));
    MDC.put("majorType", secondaryMajorId == null ? "SINGLE" : "DUAL");
  }
}
