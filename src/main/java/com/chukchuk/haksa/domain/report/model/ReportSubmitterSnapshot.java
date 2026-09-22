// 문의 생성 당시 사용자·학적 정보를 값 객체로 보존한다.

package com.chukchuk.haksa.domain.report.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 관리자 문의 화면에서 사용할 생성 시점 사용자·학적 스냅샷이다. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportSubmitterSnapshot {

  @Column(name = "submitted_user_id")
  private UUID submittedUserId;

  @Column(name = "department_id")
  private Long departmentId;

  @Column(name = "department_name")
  private String departmentName;

  @Column(name = "student_code")
  private String studentCode;

  @Column(name = "primary_major_id")
  private Long primaryMajorId;

  @Column(name = "primary_major_name")
  private String primaryMajorName;

  @Column(name = "secondary_major_id")
  private Long secondaryMajorId;

  @Column(name = "secondary_major_name")
  private String secondaryMajorName;

  @Column(name = "is_transfer_student")
  private Boolean transferStudent;

  @Column(name = "admission_year")
  private Integer admissionYear;

  @Enumerated(EnumType.STRING)
  @Column(name = "graduation_requirement_status", nullable = false, length = 20)
  private GraduationRequirementSnapshotStatus graduationRequirementStatus;

  /**
   * 사용자·학적 값과 졸업요건 확인 결과로 불변 스냅샷을 구성한다.
   *
   * @param submittedUserId 문의 생성 당시 사용자 식별자
   * @param departmentId 소속 학과 식별자
   * @param departmentName 소속 학과 이름
   * @param studentCode 학번
   * @param primaryMajorId 주전공 식별자
   * @param primaryMajorName 주전공 이름
   * @param secondaryMajorId 복수전공 식별자
   * @param secondaryMajorName 복수전공 이름
   * @param transferStudent 편입생 여부
   * @param admissionYear 입학 연도
   * @param graduationRequirementStatus 졸업요건 등록 상태
   */
  public ReportSubmitterSnapshot(
      UUID submittedUserId,
      Long departmentId,
      String departmentName,
      String studentCode,
      Long primaryMajorId,
      String primaryMajorName,
      Long secondaryMajorId,
      String secondaryMajorName,
      Boolean transferStudent,
      Integer admissionYear,
      GraduationRequirementSnapshotStatus graduationRequirementStatus) {
    this.submittedUserId = submittedUserId;
    this.departmentId = departmentId;
    this.departmentName = departmentName;
    this.studentCode = studentCode;
    this.primaryMajorId = primaryMajorId;
    this.primaryMajorName = primaryMajorName;
    this.secondaryMajorId = secondaryMajorId;
    this.secondaryMajorName = secondaryMajorName;
    this.transferStudent = transferStudent;
    this.admissionYear = admissionYear;
    this.graduationRequirementStatus = graduationRequirementStatus;
  }

  /**
   * 학생 정보가 없는 사용자의 최소 스냅샷을 생성한다.
   *
   * @param submittedUserId 문의 생성 당시 사용자 식별자
   * @return 학적 정보를 알 수 없는 스냅샷
   */
  public static ReportSubmitterSnapshot unknown(UUID submittedUserId) {
    return new ReportSubmitterSnapshot(
        submittedUserId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        GraduationRequirementSnapshotStatus.UNKNOWN);
  }

  /** 탈퇴 사용자의 직접 식별 가능한 구조화 스냅샷을 제거한다. */
  public void anonymize() {
    submittedUserId = null;
    departmentId = null;
    departmentName = null;
    studentCode = null;
    primaryMajorId = null;
    primaryMajorName = null;
    secondaryMajorId = null;
    secondaryMajorName = null;
    transferStudent = null;
    admissionYear = null;
    graduationRequirementStatus = GraduationRequirementSnapshotStatus.UNKNOWN;
  }
}
