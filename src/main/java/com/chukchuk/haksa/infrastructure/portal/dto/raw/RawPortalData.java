package com.chukchuk.haksa.infrastructure.portal.dto.raw;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 워커 payload에서 역직렬화한 학생·학기·성적 원본 데이터를 묶어 전달한다.
 *
 * @param student 포털에서 조회한 학생 기본 정보
 * @param semesters 학기별 수강 과목 원본 목록
 * @param academicRecords 학기별 성적과 누적 요약 응답
 * @param designatedCourses 포털에서 조회한 지정과목 원본 목록, 필드가 없으면 {@code null}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawPortalData(
    @JsonAlias("studentInfo") RawPortalStudentDto student,
    List<RawPortalSemesterDto> semesters,
    RawPortalGradeResponseDto academicRecords,
    List<RawPortalDesignatedCourseDto> designatedCourses) {

  /** 지정과목 필드가 없던 구버전 payload를 표현한다. */
  public RawPortalData(
      RawPortalStudentDto student,
      List<RawPortalSemesterDto> semesters,
      RawPortalGradeResponseDto academicRecords) {
    this(student, semesters, academicRecords, null);
  }
}
