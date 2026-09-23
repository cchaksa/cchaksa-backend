// dev 테스트 데이터 수정을 현재 인증 계정 범위로 처리한다

package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.application.portal.SyncDesignatedCourseService;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.admin.dto.UpdateTransferDataRequest;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.graduation.service.StudentGraduationProgressService;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseSnapshot;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개발 환경 테스트 계정의 전공과 수강 데이터를 변경한다. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminTestMutationService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final UserRepository userRepository;
  private final StudentRepository studentRepository;
  private final DepartmentRepository departmentRepository;
  private final CourseRepository courseRepository;
  private final CourseOfferingRepository courseOfferingRepository;
  private final StudentCourseRepository studentCourseRepository;
  private final AcademicCache academicCache;
  private final StudentAcademicRecordRepository academicRecordRepository;
  private final StudentGraduationProgressRepository graduationProgressRepository;
  private final StudentGraduationProgressService graduationProgressService;
  private final StudentDesignatedCourseRepository designatedCourseRepository;
  private final SyncDesignatedCourseService syncDesignatedCourseService;

  /**
   * 인증된 테스트 계정의 편입 진단 원천 데이터를 부분 변경한다.
   *
   * @param userId 인증 사용자 식별자
   * @param request 생략된 필드를 보존할 편입 데이터
   * @throws CommonException 연결된 학생이 없거나 테스트 계정이 아닌 경우
   */
  public void updateTransferData(UUID userId, UpdateTransferDataRequest request) {
    Student student = getLockedStudent(userId);
    if (!isTestAccount(student)) {
      throw new CommonException(ErrorCode.FORBIDDEN);
    }

    student.updateTransferAcademicInfo(
        request.getIsTransferStudent(), request.getCompletedSemesters());
    if (request.getTotalEarnedCredits() != null || request.getCumulativeGpa() != null) {
      StudentAcademicRecord record =
          academicRecordRepository
              .findByStudentId(student.getId())
              .orElseGet(() -> new StudentAcademicRecord(student, null, null, null, null));
      record.updateGraduationSummary(request.getTotalEarnedCredits(), request.getCumulativeGpa());
      academicRecordRepository.save(record);
    }
    if (request.getLanguageCertFulfilled() != null) {
      graduationProgressService.syncLanguageCert(student, request.getLanguageCertFulfilled());
    }
    if (request.getDesignatedCourses() != null) {
      List<UpdateTransferDataRequest.DesignatedCourseRequest> courses =
          request.getDesignatedCourses();
      List<DesignatedCourseData> data =
          IntStream.range(0, courses.size())
              .mapToObj(
                  index -> {
                    var course = courses.get(index);
                    return new DesignatedCourseData(
                        course.orgClsCd(),
                        course.subjtCd(),
                        course.subjtNm(),
                        course.point(),
                        course.precpResnCd(),
                        course.cretGainYear(),
                        course.cretSmrNm(),
                        student.getStudentCode(),
                        index);
                  })
              .toList();
      syncDesignatedCourseService.sync(
          userId, DesignatedCourseSnapshot.received(data), nextSnapshotVersion(student));
    }
    academicCache.deleteAllByStudentId(student.getId());
  }

  /**
   * 테스트 계정의 졸업 판정용 수강 과목을 요청 목록으로 교체한다.
   *
   * @param userId 사용자 식별자
   * @param request 추가·삭제할 졸업 판정용 수강 과목 목록
   */
  public void updateGraduationCourses(
      UUID userId, AdminTestDto.UpdateGraduationCoursesRequest request) {
    Student student = getRequiredStudent(userId);

    List<Long> addOfferingIds = nonNullList(request.addOfferingIds());
    if (!addOfferingIds.isEmpty()) {
      for (CourseOffering offering : courseOfferingRepository.findAllById(addOfferingIds)) {
        validateArea(request, offering);
        StudentCourse studentCourse =
            new StudentCourse(
                student,
                offering,
                new Grade(GradeType.from(request.grade())),
                request.points() != null ? request.points() : offering.getPoints(),
                Boolean.TRUE.equals(request.isRetake()),
                request.originalScore(),
                false);
        studentCourseRepository.save(studentCourse);
      }
    }

    List<Long> removeStudentCourseIds = nonNullList(request.removeStudentCourseIds());
    if (!removeStudentCourseIds.isEmpty()) {
      studentCourseRepository.deleteOwnedByStudentIdAndIdIn(
          student.getId(), removeStudentCourseIds);
    }

    academicCache.deleteAllByStudentId(student.getId());
  }

  /**
   * 테스트 계정의 주전공과 복수전공을 요청 값으로 변경한다.
   *
   * @param userId 사용자 식별자
   * @param request 변경할 주전공과 복수전공 식별정보
   * @throws CommonException 복수전공 학과가 없거나 주전공과 같은 학과인 경우
   */
  public void updateMajor(UUID userId, AdminTestDto.UpdateMajorRequest request) {
    Student student = getRequiredStudent(userId);
    Department major =
        request.majorDepartmentId() != null
            ? getDepartment(request.majorDepartmentId())
            : student.getMajor();
    Department secondaryMajor = null;

    if (request.dualMajorEnabled()) {
      if (request.secondaryMajorDepartmentId() == null) {
        throw new CommonException(ErrorCode.INVALID_ARGUMENT);
      }
      secondaryMajor = getDepartment(request.secondaryMajorDepartmentId());
      if (isSameDepartment(major, secondaryMajor)) {
        throw new CommonException(ErrorCode.INVALID_ARGUMENT);
      }
    }

    student.updateMajors(major, secondaryMajor);
    studentRepository.save(student);
    academicCache.deleteAllByStudentId(student.getId());
  }

  /**
   * 관리자 테스트 계정의 학사 데이터를 초기화한다.
   *
   * @param userId 사용자 식별자
   */
  public void resetCurrentAccount(UUID userId) {
    Student student = getLockedStudent(userId);

    studentCourseRepository.deleteByStudentId(student.getId());
    student.updateMajors(student.getDepartment(), null);
    if (isTestAccount(student)) {
      student.updateTransferAcademicInfo(false, 0);
      academicRecordRepository.deleteByStudentId(student.getId());
      graduationProgressRepository
          .findByStudentId(student.getId())
          .ifPresent(StudentGraduationProgress::clearLanguageCert);
      designatedCourseRepository.deleteAllByStudentId(student.getId());
      student.resetDesignatedCourseSnapshot(nextSnapshotVersion(student));
    }
    studentRepository.save(student);
    academicCache.deleteAllByStudentId(student.getId());
  }

  private Student getLockedStudent(UUID userId) {
    return studentRepository
        .findForUpdateByUserId(userId)
        .orElseThrow(() -> new CommonException(ErrorCode.USER_NOT_CONNECTED));
  }

  private boolean isTestAccount(Student student) {
    String studentCode = student.getStudentCode();
    User user = student.getUser();
    return studentCode != null
        && studentCode.startsWith("test_")
        && studentCode.length() > "test_".length()
        && user != null
        && !Boolean.TRUE.equals(user.getIsDeleted())
        && (studentCode + "@cchaksa.dev").equals(user.getEmail());
  }

  private Instant nextSnapshotVersion(Student student) {
    Instant version = Instant.now();
    Instant snapshot = student.getDesignatedCoursesSnapshotVersion();
    Instant reset = student.getDesignatedCoursesResetAt();
    if (snapshot != null && !version.isAfter(snapshot)) {
      version = snapshot.plusMillis(1);
    }
    if (reset != null && !version.isAfter(reset)) {
      version = reset.plusMillis(1);
    }
    return version;
  }

  /**
   * 테스트용 과목과 개설 강의를 만들고 학생의 수강 내역에 추가한다.
   *
   * @param userId 사용자 식별자
   * @param request 학생 수강 내역에 추가할 테스트 과목 정보
   * @return 생성되어 수강 내역에 추가된 테스트 과목
   * @throws CommonException 요청 또는 졸업 요건 영역이 없는 경우
   */
  public AdminTestDto.TestCourseResponse createTestCourse(
      UUID userId, AdminTestDto.CreateTestCourseRequest request) {
    if (request == null || request.area() == null) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
    Student student = getRequiredStudent(userId);
    Department department =
        request.departmentId() != null ? getDepartment(request.departmentId()) : null;
    String courseCode = resolveTestCourseCode(request.courseCode());
    String courseName = normalizeOrDefault(request.courseName(), "테스트 강의");
    Integer year = request.year() != null ? request.year() : Year.now(SEOUL).getValue();
    Integer semester = request.semester() != null ? request.semester() : 10;
    Integer credits = request.credits() != null ? request.credits() : 3;
    String hostDepartment =
        department != null
            ? department.getEstablishedDepartmentName()
            : normalize(request.hostDepartment());

    Course course = courseRepository.save(new Course(courseCode, courseName));
    CourseOffering offering =
        courseOfferingRepository.save(
            new CourseOffering(
                subjectEstablishmentSemester(year, semester),
                false,
                year,
                semester,
                hostDepartment,
                "test",
                null,
                null,
                credits,
                EvaluationType.UNKNOWN,
                request.area(),
                course,
                null,
                department,
                null));
    StudentCourse studentCourse =
        studentCourseRepository.save(
            new StudentCourse(
                student,
                offering,
                new Grade(GradeType.from(request.grade())),
                credits,
                Boolean.TRUE.equals(request.isRetake()),
                request.originalScore(),
                false));

    academicCache.deleteAllByStudentId(student.getId());
    return new AdminTestDto.TestCourseResponse(
        studentCourse.getId(),
        offering.getId(),
        course.getCourseCode(),
        course.getCourseName(),
        offering.getFacultyDivisionName());
  }

  private Student getRequiredStudent(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    Student student = user.getStudent();
    if (student == null) {
      throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
    }
    return student;
  }

  private Department getDepartment(Long departmentId) {
    return departmentRepository
        .findById(departmentId)
        .orElseThrow(() -> new CommonException(ErrorCode.INVALID_ARGUMENT));
  }

  private void validateArea(
      AdminTestDto.UpdateGraduationCoursesRequest request, CourseOffering offering) {
    if (request.area() == null || offering.getFacultyDivisionName() == null) {
      return;
    }
    if (request.area() != offering.getFacultyDivisionName()) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
  }

  private List<Long> nonNullList(List<Long> values) {
    return values != null ? values : List.of();
  }

  private boolean isSameDepartment(Department left, Department right) {
    if (left == right) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    if (left.getId() != null && right.getId() != null) {
      return Objects.equals(left.getId(), right.getId());
    }
    return left.getDepartmentCode() != null
        && Objects.equals(left.getDepartmentCode(), right.getDepartmentCode());
  }

  private String resolveTestCourseCode(String courseCode) {
    String normalized = normalize(courseCode);
    if (normalized == null) {
      return "test_" + UUID.randomUUID().toString().substring(0, 8);
    }
    if (normalized.startsWith("test_")) {
      return normalized;
    }
    return "test_" + normalized;
  }

  private String normalizeOrDefault(String value, String defaultValue) {
    String normalized = normalize(value);
    return normalized != null ? normalized : defaultValue;
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Integer subjectEstablishmentSemester(Integer year, Integer semester) {
    int semesterOrder =
        switch (semester) {
          case 20 -> 2;
          case 10 -> 1;
          case 15 -> 3;
          case 25 -> 4;
          default -> semester;
        };
    return year * 10 + semesterOrder;
  }
}
