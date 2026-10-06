package com.chukchuk.haksa.application.academic;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** 포털에서 조회한 학생의 전체 학사 기록을 표현한다. */
public class AcademicRecord {
  private UUID studentId;
  private List<SemesterGrade> semesters;
  private AcademicSummary summary;

  /**
   * 학생의 학기별 성적과 누적 요약을 묶은 학사 기록을 생성한다.
   *
   * @param studentId 학생 식별자
   * @param semesters 학생의 학기별 성적 목록
   * @param summary 학생의 누적 학사 요약
   */
  public AcademicRecord(UUID studentId, List<SemesterGrade> semesters, AcademicSummary summary) {
    this.studentId = studentId;
    this.semesters = semesters;
    this.summary = summary;
  }

  public UUID getStudentId() {
    return studentId;
  }

  /**
   * 학기별 성적을 최신 연도와 학기 순으로 정렬해 반환한다.
   *
   * @return 최신 학기부터 정렬된 성적 목록이며, 저장된 목록이 없으면 빈 목록
   */
  public List<SemesterGrade> getSemesters() {
    if (semesters == null) {
      return Collections.emptyList();
    }
    Collections.sort(
        semesters,
        (a, b) -> {
          if (b.getYear() != a.getYear()) {
            return Integer.compare(b.getYear(), a.getYear());
          }
          return Integer.compare(b.getSemester(), a.getSemester());
        });
    return semesters;
  }

  public AcademicSummary getSummary() {
    return summary;
  }
}
