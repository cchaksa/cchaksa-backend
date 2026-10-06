package com.chukchuk.haksa.infrastructure.portal.model;

/**
 * 포털 학생 정보에 포함된 현재 학년과 누적 이수 현황을 표현한다.
 *
 * @param gradeLevel 학년
 * @param completedSemesters 이수 학기 수
 * @param totalCredits 누적 취득 학점
 * @param gpa 누적 평점
 */
public record PortalAcademicInfo(
    int gradeLevel, int completedSemesters, int totalCredits, double gpa) {}
