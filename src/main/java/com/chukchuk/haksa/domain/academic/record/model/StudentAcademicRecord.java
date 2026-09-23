package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.application.academic.AcademicSummary;
import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학생의 누적 학점, 평점, 백분위 성적을 보관한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "student_academic_records")
public class StudentAcademicRecord extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "attempted_credits_gpa")
  private BigDecimal attemptedCreditsGpa;

  @Column(name = "percentile")
  private BigDecimal percentile;

  @Column(name = "cumulative_gpa")
  private BigDecimal cumulativeGpa;

  @Column(name = "total_attempted_credits")
  private Integer totalAttemptedCredits;

  @Column(name = "total_earned_credits")
  private Integer totalEarnedCredits;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", unique = true, nullable = false)
  private Student student;

  /**
   * 학생의 누적 성적 기록을 생성한다.
   *
   * @param student 성적의 소유 학생
   * @param totalAttemptedCredits 누적 신청 학점
   * @param totalEarnedCredits 누적 취득 학점
   * @param cumulativeGpa 누적 평점
   * @param percentile 누적 백분위 성적
   */
  public StudentAcademicRecord(
      Student student,
      Integer totalAttemptedCredits,
      Integer totalEarnedCredits,
      BigDecimal cumulativeGpa,
      BigDecimal percentile) {
    this.student = student;
    this.totalAttemptedCredits = totalAttemptedCredits;
    this.totalEarnedCredits = totalEarnedCredits;
    this.cumulativeGpa = cumulativeGpa;
    this.percentile = percentile;
  }

  /**
   * 포털에서 수집한 요약으로 누적 학점과 성적을 갱신한다.
   *
   * @param summary 적용할 누적 성적 요약
   */
  public void updateWith(AcademicSummary summary) {
    this.totalAttemptedCredits = summary.getTotalAttemptedCredits();
    this.totalEarnedCredits = summary.getTotalEarnedCredits();
    this.cumulativeGpa = BigDecimal.valueOf(summary.getCumulativeGpa());
    this.percentile = BigDecimal.valueOf(summary.getPercentile());
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  /**
   * 신청학점과 백분위를 보존하면서 졸업진단용 누적 성적을 부분 변경한다.
   *
   * @param earnedCredits 누적 취득학점이며 null이면 기존 값을 유지한다
   * @param gpa 누적 GPA이며 null이면 기존 값을 유지한다
   */
  public void updateGraduationSummary(Integer earnedCredits, BigDecimal gpa) {
    if (earnedCredits != null) {
      this.totalEarnedCredits = earnedCredits;
    }
    if (gpa != null) {
      this.cumulativeGpa = gpa;
    }
  }

  /**
   * 저장된 누적 학점과 성적이 주어진 요약과 같은지 확인한다.
   *
   * @param summary 비교할 누적 성적 요약
   * @return 신청·취득 학점, 누적 평점, 백분위가 모두 같으면 {@code true}
   */
  public boolean isSameAs(AcademicSummary summary) {
    return this.totalAttemptedCredits != null
        && this.totalAttemptedCredits.equals(summary.getTotalAttemptedCredits())
        && this.totalEarnedCredits != null
        && this.totalEarnedCredits.equals(summary.getTotalEarnedCredits())
        && this.cumulativeGpa != null
        && this.cumulativeGpa.compareTo(BigDecimal.valueOf(summary.getCumulativeGpa())) == 0
        && this.percentile != null
        && this.percentile.compareTo(BigDecimal.valueOf(summary.getPercentile())) == 0;
  }
}
