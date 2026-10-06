package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털이 제공한 한 학기의 학점·평점·백분위·석차를 표현한다.
 *
 * @param year 연도
 * @param semester 학기 구분
 * @param earnedCredits 취득 학점
 * @param appliedCredits 신청 학점 문자열
 * @param semesterGpa 학기 평점
 * @param score 학기 백분위 점수
 * @param ranking 석차 정보이며 제공되지 않을 수 있음
 */
public record SemesterGrade(
    int year,
    int semester,
    String earnedCredits,
    String appliedCredits,
    String semesterGpa,
    double score,
    Ranking ranking // null 가능
    ) {}
