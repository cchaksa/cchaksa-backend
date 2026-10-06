package com.chukchuk.haksa.infrastructure.portal.model;

import java.util.List;

/**
 * 특정 연도·학기에 학생이 수강한 과목 목록을 표현한다.
 *
 * @param year 연도
 * @param semester 학기 구분
 * @param courses 해당 학기의 수강 과목 목록
 */
public record SemesterCourseInfo(int year, int semester, List<CourseInfo> courses) {}
