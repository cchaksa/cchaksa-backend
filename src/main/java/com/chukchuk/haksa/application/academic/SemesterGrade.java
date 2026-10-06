package com.chukchuk.haksa.application.academic;

import lombok.Data;

/** 한 학기의 성적과 수강 과목 정보를 표현한다. */
@Data
public class SemesterGrade {

  private int year;
  private int semester;
  private int attemptedCredits;
  private int earnedCredits;
  private double semesterGpa;
  private double semesterPercentile;
  private Double attemptedCreditsGpa;
  private Integer classRank;
  private Integer totalStudents;

  /**
   * 한 학기의 이수 학점과 성적·석차 지표를 생성한다.
   *
   * @param year 연도
   * @param semester 학기 구분
   * @param attemptedCredits 신청 학점
   * @param earnedCredits 취득 학점
   * @param semesterGpa 학기 평점
   * @param semesterPercentile 해당 학기의 백분위
   * @param attemptedCreditsGpa 평점 계산에 반영된 신청 학점
   * @param classRank 해당 학기의 석차
   * @param totalStudents 석차 산정 대상 학생 수
   */
  public SemesterGrade(
      int year,
      int semester,
      int attemptedCredits,
      int earnedCredits,
      double semesterGpa,
      double semesterPercentile,
      Double attemptedCreditsGpa,
      Integer classRank,
      Integer totalStudents) {
    this.year = year;
    this.semester = semester;
    this.attemptedCredits = attemptedCredits;
    this.earnedCredits = earnedCredits;
    this.semesterGpa = semesterGpa;
    this.semesterPercentile = semesterPercentile;
    this.attemptedCreditsGpa = attemptedCreditsGpa;
    this.classRank = classRank;
    this.totalStudents = totalStudents;
  }
}
