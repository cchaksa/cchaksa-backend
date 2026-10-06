package com.chukchuk.haksa.infrastructure.portal.model;

import java.util.List;

/**
 * 포털 교육과정에서 조회한 과목·교수·개설 과목 목록을 묶어 표현한다.
 *
 * @param courses 학생 수강 과목 목록
 * @param professors 과목에 연결할 교수 목록
 * @param offerings 연도·학기별 개설 과목 목록
 */
public record PortalCurriculumData(
    List<CourseInfo> courses, List<ProfessorInfo> professors, List<OfferingInfo> offerings) {}
