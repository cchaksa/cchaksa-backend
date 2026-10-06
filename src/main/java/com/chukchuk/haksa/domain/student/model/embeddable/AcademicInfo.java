package com.chukchuk.haksa.domain.student.model.embeddable;

import com.chukchuk.haksa.domain.student.model.StudentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학생의 입학 연도·학기·유형과 현재 등록 학기를 묶어 저장한다. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class AcademicInfo {

  @Column(name = "admission_year", nullable = false)
  private Integer admissionYear;

  @Column(name = "semester_enrolled")
  private Integer semesterEnrolled;

  @Column(name = "is_transfer_student")
  private Boolean isTransferStudent;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private StudentStatus status;

  @Column(name = "grade_level")
  private Integer gradeLevel;

  @Column(name = "completed_semesters")
  private Integer completedSemesters;

  // Builder 패턴 추가
  /**
   * 입학 연도와 등록·이수 학기 및 재학 상태를 묶어 학적 정보를 생성한다.
   *
   * @param admissionYear admission 연도
   * @param semesterEnrolled 현재까지 등록한 학기 수
   * @param isTransferStudent 편입생인지 여부
   * @param status 상태
   * @param gradeLevel 학년
   * @param completedSemesters 이수 학기 수
   */
  @Builder
  public AcademicInfo(
      Integer admissionYear,
      Integer semesterEnrolled,
      Boolean isTransferStudent,
      StudentStatus status,
      Integer gradeLevel,
      Integer completedSemesters) {
    this.admissionYear = admissionYear;
    this.semesterEnrolled = semesterEnrolled;
    this.isTransferStudent = isTransferStudent;
    this.status = status;
    this.gradeLevel = gradeLevel;
    this.completedSemesters = completedSemesters;
  }
}
