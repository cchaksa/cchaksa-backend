package com.chukchuk.haksa.domain.lectureevaluations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 강의평가 기능의 설정값을 제공한다. */
@Component
@ConfigurationProperties(prefix = "lecture-evaluation")
public class LectureEvaluationProperties {

  private Integer targetYear;
  private Integer targetSemester;

  /** Spring이 강의평가 설정값을 바인딩할 때 사용하는 기본 생성자다. */
  public LectureEvaluationProperties() {}

  /**
   * 강의평가 대상 연도와 학기로 설정을 구성한다.
   *
   * @param targetYear 강의평가 대상 연도
   * @param targetSemester 강의평가 대상 학기
   */
  public LectureEvaluationProperties(Integer targetYear, Integer targetSemester) {
    this.targetYear = targetYear;
    this.targetSemester = targetSemester;
  }

  public Integer getTargetYear() {
    return targetYear;
  }

  public void setTargetYear(Integer targetYear) {
    this.targetYear = targetYear;
  }

  public Integer getTargetSemester() {
    return targetSemester;
  }

  public void setTargetSemester(Integer targetSemester) {
    this.targetSemester = targetSemester;
  }
}
