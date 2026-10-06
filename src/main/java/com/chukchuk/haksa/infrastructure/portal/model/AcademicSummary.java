package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털이 제공한 누적 신청·취득 학점과 성적 지표를 표현한다.
 *
 * @param appliedCredits 누적 신청 학점
 * @param totalCredits 누적 취득 학점
 * @param gpa 누적 평점
 * @param score 누적 백분위 점수
 */
public record AcademicSummary(int appliedCredits, int totalCredits, double gpa, double score) {}
