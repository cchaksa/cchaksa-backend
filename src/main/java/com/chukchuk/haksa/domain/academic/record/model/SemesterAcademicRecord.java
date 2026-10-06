package com.chukchuk.haksa.domain.academic.record.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학생의 한 학기 성적, 취득 학점, 강의평가 상태를 보관한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "semester_academic_records",
    indexes = {
      @Index(name = "idx_student_year_semester", columnList = "student_id, year, semester")
    })
@Access(AccessType.FIELD)
public class SemesterAcademicRecord extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "semester", nullable = false)
  private Integer semester;

  @Column(name = "year")
  private Integer year;

  @Column(name = "total_students")
  private Integer totalStudents;

  @Column(name = "class_rank")
  private Integer classRank;

  @Column(name = "attempted_credits_gpa")
  private BigDecimal attemptedCreditsGpa;

  @Column(name = "semester_percentile")
  private BigDecimal semesterPercentile;

  @Column(name = "semester_gpa")
  private BigDecimal semesterGpa;

  @Column(name = "attempted_credits")
  private Integer attemptedCredits;

  @Column(name = "earned_credits")
  private Integer earnedCredits;

  @Enumerated(EnumType.STRING)
  @Column(name = "lecture_evaluation_status")
  private LectureEvaluationStatus lectureEvaluationStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  /**
   * 학생의 학기 성적 기록을 생성한다.
   *
   * @param student 성적의 소유 학생
   * @param year 연도
   * @param semester 성적이 속한 학기
   * @param attemptedCredits 신청 학점
   * @param earnedCredits 취득 학점
   * @param semesterGpa 학기 평점
   * @param semesterPercentile 학기 백분위 성적
   * @param attemptedCreditsGpa 평점 계산에 포함된 신청 학점
   * @param classRank 학기 석차
   * @param totalStudents 석차 산정 대상 인원
   */
  public SemesterAcademicRecord(
      Student student,
      Integer year,
      Integer semester,
      Integer attemptedCredits,
      Integer earnedCredits,
      BigDecimal semesterGpa,
      BigDecimal semesterPercentile,
      BigDecimal attemptedCreditsGpa,
      Integer classRank,
      Integer totalStudents) {
    this.student = student;
    this.year = year;
    this.semester = semester;
    this.attemptedCredits = attemptedCredits;
    this.earnedCredits = earnedCredits;
    this.semesterGpa = semesterGpa;
    this.semesterPercentile = semesterPercentile;
    this.attemptedCreditsGpa = attemptedCreditsGpa;
    this.classRank = classRank;
    this.totalStudents = totalStudents;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  public Integer getSemester() {
    return this.semester;
  }

  private boolean compareBigDecimal(BigDecimal a, BigDecimal b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return a.compareTo(b) == 0;
  }

  /**
   * 학기 학사 기록의 주요 내용이 같은지 비교한다.
   *
   * @param other 비교할 다른 학기 성적, {@code null}이면 다른 기록으로 간주함
   * @return 비교 대상 성적 필드가 모두 같으면 {@code true}
   */
  public boolean equalsContentOf(SemesterAcademicRecord other) {
    if (other == null) {
      return false;
    }

    return Objects.equals(this.year, other.year)
        && Objects.equals(this.semester, other.semester)
        && Objects.equals(this.totalStudents, other.totalStudents)
        && Objects.equals(this.classRank, other.classRank)
        && Objects.equals(this.attemptedCredits, other.attemptedCredits)
        && Objects.equals(this.earnedCredits, other.earnedCredits)
        && compareBigDecimal(this.semesterGpa, other.semesterGpa)
        && compareBigDecimal(this.semesterPercentile, other.semesterPercentile)
        && compareBigDecimal(this.attemptedCreditsGpa, other.attemptedCreditsGpa);
  }

  /**
   * 학생 연결과 강의평가 상태를 유지한 채 성적 내용을 주어진 기록으로 교체한다.
   *
   * @param src 복사할 학기 성적 기록
   */
  public void updateWith(SemesterAcademicRecord src) {
    this.year = src.year;
    this.semester = src.semester;
    this.totalStudents = src.totalStudents;
    this.classRank = src.classRank;
    this.attemptedCreditsGpa = src.attemptedCreditsGpa;
    this.semesterPercentile = src.semesterPercentile;
    this.semesterGpa = src.semesterGpa;
    this.attemptedCredits = src.attemptedCredits;
    this.earnedCredits = src.earnedCredits;
  }

  /** 강의평가 상태를 제출 대기로 변경한다. */
  public void markLectureEvaluationPending() {
    if (this.lectureEvaluationStatus == LectureEvaluationStatus.SKIPPED
        || this.lectureEvaluationStatus == LectureEvaluationStatus.COMPLETED) {
      return;
    }
    this.lectureEvaluationStatus = LectureEvaluationStatus.PENDING;
  }

  /** 강의평가 상태를 미공개로 변경한다. */
  public void markLectureEvaluationNotReleased() {
    if (this.lectureEvaluationStatus != null) {
      return;
    }
    this.lectureEvaluationStatus = LectureEvaluationStatus.NOT_RELEASED;
  }

  /** 강의평가 상태를 건너뜀으로 변경한다. */
  public void markLectureEvaluationSkipped() {
    if (isLectureEvaluationPending()) {
      this.lectureEvaluationStatus = LectureEvaluationStatus.SKIPPED;
    }
  }

  /** 강의평가 상태를 제출 완료로 변경한다. */
  public void markLectureEvaluationCompleted() {
    this.lectureEvaluationStatus = LectureEvaluationStatus.COMPLETED;
  }

  public void setLectureEvaluationStatusForTest(LectureEvaluationStatus status) {
    this.lectureEvaluationStatus = status;
  }

  /**
   * 강의평가를 제출하거나 건너뛰어야 하는 대기 상태인지 확인한다.
   *
   * @return 강의평가 상태가 {@code PENDING}이면 {@code true}
   */
  public boolean isLectureEvaluationPending() {
    return this.lectureEvaluationStatus == LectureEvaluationStatus.PENDING;
  }
}
