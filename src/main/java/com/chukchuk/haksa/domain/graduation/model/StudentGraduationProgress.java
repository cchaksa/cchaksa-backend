package com.chukchuk.haksa.domain.graduation.model;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.student.model.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학생 졸업 progress 도메인 상태를 표현한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "student_graduation_progress")
public class StudentGraduationProgress extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "elective_courses_fulfilled")
  private Boolean electiveCoursesFulfilled;

  @Column(name = "credits_fulfilled")
  private Boolean creditsFulfilled;

  @Column(name = "language_cert_fulfilled")
  private Boolean languageCertFulfilled;

  @Column(name = "graduation_review_fulfilled")
  private Boolean graduationReviewFulfilled;

  @Column(name = "gpa_fulfilled")
  private Boolean gpaFulfilled;

  @Column(name = "checked_at")
  private Instant checkedAt;

  @Column(name = "area_requirements_fulfilled")
  private Boolean areaRequirementsFulfilled;

  @Enumerated(EnumType.STRING)
  @Column(name = "graduation_status")
  private GraduationStatus graduationStatus;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id")
  private Student student;

  private StudentGraduationProgress(Student student, Boolean languageCertFulfilled) {
    this.student = student;
    this.languageCertFulfilled = languageCertFulfilled;
  }

  /**
   * 학생의 외국어 인증 충족 상태를 반영한 졸업 진행도를 생성한다.
   *
   * @param student 대상 학생
   * @param languageCertFulfilled 어학 인증 충족 여부
   * @return 외국어 인증 충족 상태를 반영한 학생 졸업 진행도
   */
  public static StudentGraduationProgress createForLanguageCert(
      Student student, boolean languageCertFulfilled) {
    return new StudentGraduationProgress(student, languageCertFulfilled);
  }

  /**
   * 포털 외부의 수동 졸업심사 정보를 저장할 진행 상태를 생성한다.
   *
   * @param student 대상 학생
   * @return 수동 졸업심사 상태를 담은 진행 상태
   */
  public static StudentGraduationProgress createForManualReview(Student student) {
    return new StudentGraduationProgress(student, null);
  }

  /**
   * 졸업 요건의 호출자에게 노출된 상태를 입력 값에 맞게 변경한다.
   *
   * @param languageCertFulfilled 어학 인증 충족 여부
   */
  public void updateLanguageCert(boolean languageCertFulfilled) {
    this.languageCertFulfilled = languageCertFulfilled;
  }

  /** 다른 졸업 진행 상태를 보존하고 외국어 인증만 미확인 상태로 되돌린다. */
  public void clearLanguageCert() {
    this.languageCertFulfilled = null;
  }

  /**
   * 학과 졸업논문·시험·작품·실기 심사 통과 여부를 수동으로 저장한다.
   *
   * @param graduationReviewFulfilled 졸업심사 통과 여부
   */
  public void updateGraduationReview(Boolean graduationReviewFulfilled) {
    this.graduationReviewFulfilled = graduationReviewFulfilled;
  }
}
