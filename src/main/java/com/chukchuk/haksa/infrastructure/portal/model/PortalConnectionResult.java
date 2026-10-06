package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털 학생 정보의 연결 성공 여부와 실패 사유를 전달한다.
 *
 * @param isSuccess 연결 정보가 저장됐는지 여부
 * @param studentId 연결된 학생의 학번이며 실패하면 {@code null}
 * @param studentInfo 연결된 학생 요약이며 실패하면 {@code null}
 * @param error 연결 실패 사유이며 성공하면 {@code null}
 */
public record PortalConnectionResult(
    boolean isSuccess, String studentId, StudentInfo studentInfo, String error) {
  /**
   * 성공 결과를 생성한다.
   *
   * @param studentId 연결된 학생의 학번
   * @param studentInfo 연결된 학생 요약
   * @return 학생 정보가 포함된 성공 결과
   */
  public static PortalConnectionResult success(String studentId, StudentInfo studentInfo) {
    return new PortalConnectionResult(true, studentId, studentInfo, null);
  }

  /**
   * 실패 사유를 포함한 결과를 생성한다.
   *
   * @param error 호출자에게 전달할 연결 실패 사유
   * @return 학생 정보 없이 실패 사유만 포함한 결과
   */
  public static PortalConnectionResult failure(String error) {
    return new PortalConnectionResult(false, null, null, error);
  }

  /**
   * 포털 연동 응답에 노출할 학생의 소속과 학적 요약을 표현한다.
   *
   * @param name 이름
   * @param school 학교 이름
   * @param majorName 전공 이름
   * @param studentCode 학번
   * @param gradeLevel 학년
   * @param status 상태
   * @param completedSemesterType 이수 학기 수에서 계산한 현재 학기 구분
   */
  public record StudentInfo(
      String name,
      String school,
      String majorName,
      String studentCode,
      int gradeLevel,
      String status,
      int completedSemesterType) {}
}
