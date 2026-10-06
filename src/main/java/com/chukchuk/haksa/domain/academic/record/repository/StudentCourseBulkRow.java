package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.domain.student.model.GradeType;
import java.util.UUID;

/**
 * 수강 내역 일괄 삽입에 필요한 열 값을 표현한다.
 *
 * @param studentId 학생 식별자
 * @param offeringId 과목 개설 식별자
 * @param gradeType 배치 저장할 성적 유형
 * @param points 학점
 * @param isRetake 재수강 과목인지 여부
 * @param originalScore 원점수
 * @param isRetakeDeleted 재수강으로 대체돼 성적 계산에서 제외되는지 여부
 */
public record StudentCourseBulkRow(
    UUID studentId,
    Long offeringId,
    GradeType gradeType,
    Integer points,
    boolean isRetake,
    Integer originalScore,
    boolean isRetakeDeleted) {

  /**
   * 수강 정보를 JDBC 배치 저장 행으로 변환한다.
   *
   * @param enrollment 배치 저장할 학생 수강 정보
   * @return 수강 내역을 일괄 삽입 열 값으로 변환한 결과
   */
  public static StudentCourseBulkRow from(CourseEnrollment enrollment) {
    Integer normalizedScore =
        enrollment.getOriginalScore() != null ? enrollment.getOriginalScore().intValue() : null;
    return new StudentCourseBulkRow(
        enrollment.getStudentId(),
        enrollment.getOfferingId(),
        enrollment.getGradeType(),
        enrollment.getPoints(),
        enrollment.isRetake(),
        normalizedScore,
        enrollment.isRetakeDeleted());
  }
}
