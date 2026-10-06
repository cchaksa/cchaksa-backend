package com.chukchuk.haksa.infrastructure.portal.mapper;

import com.chukchuk.haksa.application.academic.AcademicSummary;
import com.chukchuk.haksa.application.academic.SemesterGrade;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.student.model.Student;
import java.math.BigDecimal;

/** 애플리케이션 학사 요약과 학기 성적을 영속 학사 기록 엔티티로 변환한다. */
public class AcademicRecordMapper {

  /**
   * 학생과 누적 학사 요약을 학생 학사 기록 엔티티로 변환한다.
   *
   * @param student 학사 기록의 소유 학생
   * @param summary 누적 신청·취득 학점과 성적 요약
   * @return null 성적 지표를 0으로 보정한 학생 학사 기록
   * @throws IllegalArgumentException 학사 요약이 {@code null}인 경우
   */
  public static StudentAcademicRecord toEntity(Student student, AcademicSummary summary) {

    if (summary == null) {
      throw new IllegalArgumentException("AcademicSummary cannot be null");
    }

    // null 체크를 통한 안전한 변환
    BigDecimal cumulativeGpa =
        summary.getCumulativeGpa() != null
            ? BigDecimal.valueOf(summary.getCumulativeGpa())
            : BigDecimal.ZERO;

    BigDecimal percentile =
        summary.getPercentile() != null
            ? BigDecimal.valueOf(summary.getPercentile())
            : BigDecimal.ZERO;

    return new StudentAcademicRecord(
        student,
        summary.getTotalAttemptedCredits(),
        summary.getTotalEarnedCredits(),
        cumulativeGpa,
        percentile);
  }

  /**
   * 학생과 한 학기의 성적을 학기 학사 기록 엔티티로 변환한다.
   *
   * @param student 학기 기록의 소유 학생
   * @param grade 연도·학기·학점·성적·석차 정보
   * @return 평점 반영 학점의 null을 0으로 보정한 학기 학사 기록
   * @throws IllegalArgumentException 학기 성적이 {@code null}인 경우
   */
  public static SemesterAcademicRecord toEntity(Student student, SemesterGrade grade) {
    if (grade == null) {
      throw new IllegalArgumentException("SemesterGrade cannot be null");
    }

    BigDecimal attemptedCreditsGpa =
        grade.getAttemptedCreditsGpa() != null
            ? BigDecimal.valueOf(grade.getAttemptedCreditsGpa())
            : BigDecimal.ZERO;

    return new SemesterAcademicRecord(
        student,
        grade.getYear(),
        grade.getSemester(),
        grade.getAttemptedCredits(),
        grade.getEarnedCredits(),
        BigDecimal.valueOf(grade.getSemesterGpa()),
        BigDecimal.valueOf(grade.getSemesterPercentile()),
        attemptedCreditsGpa,
        grade.getClassRank(),
        grade.getTotalStudents());
  }
}
