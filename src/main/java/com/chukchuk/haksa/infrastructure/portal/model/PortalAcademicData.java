package com.chukchuk.haksa.infrastructure.portal.model;

import java.util.List;

/**
 * 포털에서 조회한 학기별 과목과 성적 요약을 묶어 표현한다.
 *
 * @param semesters 학기별 수강 과목 목록
 * @param grades 학기별·누적 성적 정보
 * @param summary 누적 학점과 평점 요약
 */
public record PortalAcademicData(
    List<SemesterCourseInfo> semesters, GradeSummary grades, AcademicSummary summary) {}
