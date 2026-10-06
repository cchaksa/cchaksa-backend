package com.chukchuk.haksa.domain.cache;

import java.util.UUID;

/** 학사 데이터 캐시 키를 일관된 형식으로 생성한다. */
public final class AcademicCacheKeys {

  private AcademicCacheKeys() {}

  /**
   * 학생별 학사 요약 캐시 키를 반환한다.
   *
   * @param studentId 학생 식별자
   * @return {@code student:{studentId}:summary} 형식의 캐시 키
   */
  public static String summary(UUID studentId) {
    return "student:" + studentId + ":summary";
  }

  /**
   * 학생별 학기 목록 캐시 키를 반환한다.
   *
   * @param studentId 학생 식별자
   * @return {@code student:{studentId}:semesters} 형식의 캐시 키
   */
  public static String semesters(UUID studentId) {
    return "student:" + studentId + ":semesters";
  }

  /**
   * 학생별 졸업 진행 상태 캐시 키를 반환한다.
   *
   * @param studentId 학생 식별자
   * @return {@code student:{studentId}:graduation} 형식의 캐시 키
   */
  public static String graduation(UUID studentId) {
    return "student:" + studentId + ":graduation";
  }

  /**
   * 학과와 입학 연도별 졸업 요건 캐시 키를 반환한다.
   *
   * @param departmentId 학과 식별자
   * @param admissionYear 입학 연도
   * @return {@code graduation:requirements:{departmentId}:{admissionYear}} 형식의 키
   */
  public static String graduationRequirements(Long departmentId, Integer admissionYear) {
    return "graduation:requirements:" + departmentId + ":" + admissionYear;
  }

  /**
   * 복수전공 졸업요건 캐시 키를 반환한다.
   *
   * @param primaryMajorId 주전공 식별자
   * @param secondaryMajorId 복수전공 식별자
   * @param admissionYear 입학 연도
   * @return 주전공·복수전공·입학 연도가 포함된 졸업 요건 캐시 키
   */
  public static String dualGraduationRequirements(
      Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
    return "graduation:dual-requirements:"
        + primaryMajorId
        + ":"
        + secondaryMajorId
        + ":"
        + admissionYear;
  }

  /**
   * 학생별 학기 성적 요약 캐시 키를 반환한다.
   *
   * @param studentId 학생 식별자
   * @return {@code student:{studentId}:semester-summaries} 형식의 캐시 키
   */
  public static String semesterSummaries(UUID studentId) {
    return "student:" + studentId + ":semester-summaries";
  }

  /**
   * 학생의 모든 학사 캐시를 찾기 위한 키 접두사를 반환한다.
   *
   * @param studentId 학생 식별자
   * @return {@code student:{studentId}:} 형식의 캐시 키 접두사
   */
  public static String studentPrefix(UUID studentId) {
    return "student:" + studentId + ":";
  }
}
