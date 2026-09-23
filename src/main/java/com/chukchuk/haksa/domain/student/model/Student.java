package com.chukchuk.haksa.domain.student.model;

import static jakarta.persistence.CascadeType.REMOVE;

import com.chukchuk.haksa.domain.BaseEntity;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자의 학적·전공·학사 기록과 포털 동기화 상태를 관리한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "students")
public class Student extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "student_id")
  private UUID id;

  @Column(name = "student_code", nullable = false, unique = true)
  private String studentCode;

  @Column(name = "name")
  private String name;

  @Column(name = "is_graduated")
  private Boolean isGraduated;

  @Column(name = "admission_type")
  private String admissionType;

  @Column(name = "target_gpa")
  private Double targetGpa;

  @Column(name = "designated_courses_snapshot_version")
  private Instant designatedCoursesSnapshotVersion;

  @Column(name = "designated_courses_reset_at")
  private Instant designatedCoursesResetAt;

  @Column(name = "transfer_registered_semesters")
  private Integer transferRegisteredSemesters;

  @Column(name = "reconnection_required", nullable = false)
  private boolean reconnectionRequired = true; // 기본값 true, 재연동 시 false

  @Embedded private AcademicInfo academicInfo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "major_id")
  private Department major;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "secondary_major_id")
  private Department secondaryMajor;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  //    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true, fetch =
  // FetchType.LAZY)
  //    private StudentAcademicRecord studentAcademicRecord;

  @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SemesterAcademicRecord> semesterAcademicRecords = new ArrayList<>();

  @OneToMany(
      mappedBy = "student",
      cascade = {CascadeType.PERSIST, REMOVE},
      orphanRemoval = true)
  private List<StudentCourse> studentCourses = new ArrayList<>();

  /**
   * 학번과 소속·학적·사용자 연결을 포함한 학생 엔티티를 생성한다.
   *
   * @param studentCode 학번
   * @param name 이름
   * @param department 소속 학과
   * @param major 주전공
   * @param secondaryMajor 복수전공
   * @param admissionYear admission 연도
   * @param semesterEnrolled 현재 등록 학기 차수
   * @param isTransferStudent 편입생인지 여부
   * @param isGraduated 졸업 상태인지 여부
   * @param status 상태
   * @param gradeLevel 학년
   * @param completedSemesters 이수 학기 수
   * @param admissionType 신입학·편입학 등 입학 유형
   * @param user 연결할 사용자
   */
  @Builder
  public Student(
      String studentCode,
      String name,
      Department department,
      Department major,
      Department secondaryMajor,
      Integer admissionYear,
      Integer semesterEnrolled,
      Boolean isTransferStudent,
      Boolean isGraduated,
      StudentStatus status,
      Integer gradeLevel,
      Integer completedSemesters,
      String admissionType,
      User user) {
    this.studentCode = studentCode;
    this.name = name;
    this.department = department;
    this.major = major;
    this.secondaryMajor = secondaryMajor;
    this.isGraduated = isGraduated;
    this.admissionType = admissionType;

    // Builder 패턴을 이용해 AcademicInfo 객체 생성
    this.academicInfo =
        AcademicInfo.builder()
            .admissionYear(admissionYear)
            .semesterEnrolled(semesterEnrolled)
            .isTransferStudent(isTransferStudent)
            .status(status) // Enum으로 변환
            .gradeLevel(gradeLevel)
            .completedSemesters(completedSemesters)
            .build();

    this.user = user;
  }

  /**
   * 포털에서 수집한 최신 학적 정보로 학생의 소속·학적 상태를 갱신한다.
   *
   * @param name 이름
   * @param department 학생의 소속 학과
   * @param major 학생의 주전공
   * @param secondaryMajor 학생의 복수전공
   * @param admissionYear 입학 연도
   * @param semesterEnrolled 현재 등록 학기
   * @param isTransferStudent 편입생 여부
   * @param isGraduated 졸업 여부
   * @param status 상태
   * @param gradeLevel 학년
   * @param completedSemesters 이수 학기 수
   * @param admissionType 입학 전형
   */
  public void updateInfo(
      String name,
      Department department,
      Department major,
      Department secondaryMajor,
      Integer admissionYear,
      Integer semesterEnrolled,
      Boolean isTransferStudent,
      Boolean isGraduated,
      StudentStatus status,
      Integer gradeLevel,
      Integer completedSemesters,
      String admissionType) {

    this.name = name;
    this.department = department;
    this.major = major;
    this.secondaryMajor = secondaryMajor;
    this.isGraduated = isGraduated;
    this.admissionType = admissionType;

    this.academicInfo =
        AcademicInfo.builder()
            .admissionYear(admissionYear)
            .semesterEnrolled(semesterEnrolled)
            .isTransferStudent(isTransferStudent)
            .status(status)
            .gradeLevel(gradeLevel)
            .completedSemesters(completedSemesters)
            .build();
  }

  /**
   * 저장된 학적 정보로 편입생 여부를 확인한다.
   *
   * @return 저장된 편입생 플래그가 {@code true}이면 {@code true}
   */
  public boolean isTransferStudent() {
    return this.academicInfo != null
        && Boolean.TRUE.equals(this.academicInfo.getIsTransferStudent());
  }

  /**
   * 입학연도와 전공을 보존하면서 편입 여부와 이수 학기를 부분 변경한다.
   *
   * @param transferStudent 편입 여부이며 null이면 기존 값을 유지한다
   * @param completedSemesters 이수 학기 수이며 null이면 기존 값을 유지한다
   */
  public void updateTransferAcademicInfo(Boolean transferStudent, Integer completedSemesters) {
    if (transferStudent == null && completedSemesters == null) {
      return;
    }
    this.academicInfo =
        AcademicInfo.builder()
            .admissionYear(academicInfo.getAdmissionYear())
            .semesterEnrolled(academicInfo.getSemesterEnrolled())
            .isTransferStudent(
                transferStudent != null ? transferStudent : academicInfo.getIsTransferStudent())
            .status(academicInfo.getStatus())
            .gradeLevel(academicInfo.getGradeLevel())
            .completedSemesters(
                completedSemesters != null
                    ? completedSemesters
                    : academicInfo.getCompletedSemesters())
            .build();
    if (transferStudent != null) {
      this.admissionType = transferStudent ? "2" : "신입";
    }
  }

  /**
   * 요청한 지정과목 스냅샷이 저장된 버전과 마지막 초기화 시점보다 최신인지 확인한다.
   *
   * @param requestedVersion 비교할 스냅샷 버전
   * @return 두 기준 시점이 없거나 요청 버전이 모두 더 최신이면 {@code true}
   */
  public boolean canApplyDesignatedCourseSnapshot(Instant requestedVersion) {
    return isAfterWatermark(requestedVersion, designatedCoursesSnapshotVersion)
        && isAfterWatermark(requestedVersion, designatedCoursesResetAt);
  }

  private boolean isAfterWatermark(Instant requestedVersion, Instant watermark) {
    return watermark == null || requestedVersion.isAfter(watermark);
  }

  /**
   * 지정과목 스냅샷의 마지막 반영 버전을 갱신한다.
   *
   * @param snapshotVersion 반영한 스냅샷 버전
   */
  public void updateDesignatedCourseSnapshotVersion(Instant snapshotVersion) {
    this.designatedCoursesSnapshotVersion = snapshotVersion;
  }

  /** 지정과목 스냅샷의 마지막 반영 버전을 초기화한다. */
  public void clearDesignatedCourseSnapshotVersion() {
    this.designatedCoursesSnapshotVersion = null;
  }

  /**
   * 지정과목을 초기화하고 초기화 시점 이전의 스냅샷을 차단한다.
   *
   * @param resetAt 늦은 스냅샷을 차단할 기준 시각
   */
  public void resetDesignatedCourseSnapshot(Instant resetAt) {
    this.designatedCoursesResetAt = resetAt;
    clearDesignatedCourseSnapshotVersion();
  }

  /**
   * 학생에게 수강 과목을 연결한다.
   *
   * @param course 연결할 수강 과목
   */
  public void addStudentCourse(StudentCourse course) {
    this.studentCourses.add(course);
    course.setStudent(this);
  }

  // 연관관계 편의 메서드
  /**
   * 전달된 값을 현재 객체에 설정한다.
   *
   * @param record 연결할 학사 기록
   */
  public void setAcademicRecord(StudentAcademicRecord record) {
    //        this.studentAcademicRecord = record;
    if (record != null) {
      record.setStudent(this);
    }
  }

  /**
   * 학생에게 학기별 학사 기록을 연결한다.
   *
   * @param record 연결할 학사 기록
   */
  public void addSemesterRecord(SemesterAcademicRecord record) {
    this.semesterAcademicRecords.add(record);
    record.setStudent(this);
  }

  public void setTargetGpa(Double targetGpa) {
    this.targetGpa = targetGpa;
  }

  /**
   * 포털에서 확인할 수 없는 편입 후 등록 학기 수를 수동으로 저장한다.
   *
   * @param registeredSemesters 편입 후 등록 학기 수
   */
  public void updateTransferRegisteredSemesters(Integer registeredSemesters) {
    this.transferRegisteredSemesters = registeredSemesters;
  }

  /**
   * 학생의 주전공과 복수전공 연결을 교체한다.
   *
   * @param major 학생의 주전공
   * @param secondaryMajor 학생의 복수전공
   */
  public void updateMajors(Department major, Department secondaryMajor) {
    this.major = major;
    this.secondaryMajor = secondaryMajor;
  }

  // 학생 정보 업데이트 시 변경 사항 감지 메서드
  /**
   * 새 학적 정보로 학생 정보를 갱신해야 하는지 확인한다.
   *
   * @param newData 포털에서 새로 수집한 학생 학적 정보
   * @return 저장된 학적 필드 중 하나라도 다르면 {@code true}
   */
  public boolean needsUpdate(StudentInitializationDataType newData) {
    if (!equalsNullable(this.name, newData.getName())) {
      return true;
    }
    if (!equalsNullable(this.department, newData.getDepartment())) {
      return true;
    }
    if (!equalsNullable(this.major, newData.getMajor())) {
      return true;
    }
    if (!equalsNullable(this.secondaryMajor, newData.getSecondaryMajor())) {
      return true;
    }
    if (!equalsNullable(this.isGraduated, newData.isGraduated())) {
      return true;
    }
    if (!equalsNullable(this.admissionType, newData.getAdmissionType())) {
      return true;
    }

    AcademicInfo newInfo =
        AcademicInfo.builder()
            .admissionYear(newData.getAdmissionYear())
            .semesterEnrolled(newData.getSemesterEnrolled())
            .isTransferStudent(newData.isTransferStudent())
            .status(newData.getStatus())
            .gradeLevel(newData.getGradeLevel())
            .completedSemesters(newData.getCompletedSemesters())
            .build();

    return !equalsNullable(this.academicInfo, newInfo);
  }

  private boolean equalsNullable(Object a, Object b) {
    return java.util.Objects.equals(a, b);
  }

  /** 학생의 포털 재연동 상태를 기록한다. */
  public void markReconnected() {
    this.reconnectionRequired = false;
  }

  /** 탈퇴한 사용자의 학번과 이름을 익명화한다. */
  public void anonymize() {
    this.studentCode = "deleted_" + UUID.randomUUID();
    this.name = "탈퇴한 사용자입니다.";
  }

  /**
   * 연결된 사용자 참조를 새 사용자로 교체한다.
   *
   * @param user 대상 사용자
   */
  public void updateUser(User user) {
    this.user = user;
  }
}
