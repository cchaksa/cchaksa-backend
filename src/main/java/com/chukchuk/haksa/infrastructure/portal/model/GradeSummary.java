package com.chukchuk.haksa.infrastructure.portal.model;

import java.util.List;

/**
 * 포털의 학기별 성적과 누적 성적 요약을 묶어 표현한다.
 *
 * @param semesters 학기별 성적 목록
 * @param summary 누적 학점과 성적 요약
 */
public record GradeSummary(List<SemesterGrade> semesters, AcademicSummary summary) {}
