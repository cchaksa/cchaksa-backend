package com.chukchuk.haksa.application.academic.enrollment;

import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import java.util.UUID;
import lombok.Getter;

/** 학생의 개설 과목 수강 결과와 재수강 상태를 표현한다. */
@Getter
public class CourseEnrollment {
  private UUID studentId;
  private Long offeringId;
  private Grade grade;
  private Integer points;
  private boolean isRetake;
  private Double originalScore;
  private boolean isRetakeDeleted;

  /**
   * 학생의 개설 과목 성적과 재수강 정보를 생성한다.
   *
   * @param studentId 학생 식별자
   * @param offeringId 과목 개설 식별자
   * @param grade 취득 성적
   * @param points 학점
   * @param isRetake 재수강 여부
   * @param originalScore 원점수
   * @param isRetakeDeleted 재수강으로 대체된 성적의 제외 여부
   */
  public CourseEnrollment(
      UUID studentId,
      Long offeringId,
      Grade grade,
      Integer points,
      boolean isRetake,
      Double originalScore,
      boolean isRetakeDeleted) {
    this.studentId = studentId;
    this.offeringId = offeringId;
    this.grade = grade;
    this.points = points;
    this.isRetake = isRetake;
    this.originalScore = originalScore;
    this.isRetakeDeleted = isRetakeDeleted;
  }

  public GradeType getGradeType() {
    return grade.getValue();
  }
}
