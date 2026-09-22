package com.chukchuk.haksa.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.auth.entity.RefreshToken;
import com.chukchuk.haksa.domain.auth.repository.RefreshTokenRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.model.StudentGraduationProgress;
import com.chukchuk.haksa.domain.graduation.repository.StudentGraduationProgressRepository;
import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import com.chukchuk.haksa.domain.lectureevaluations.model.LectureEvaluationTag;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationRepository;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationTagRepository;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.professor.repository.ProfessorRepository;
import com.chukchuk.haksa.domain.report.model.GraduationRequirementSnapshotStatus;
import com.chukchuk.haksa.domain.report.model.Report;
import com.chukchuk.haksa.domain.report.model.ReportSubmitterSnapshot;
import com.chukchuk.haksa.domain.report.repository.ReportRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.SocialAccount;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.SocialAccountRepository;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

  @Autowired private UserService userService;
  @Autowired private UserRepository userRepository;
  @Autowired private SocialAccountRepository socialAccountRepository;
  @SpyBean private StudentRepository studentRepository;
  @SpyBean private StudentAcademicRecordRepository studentAcademicRecordRepository;
  @Autowired private SemesterAcademicRecordRepository semesterAcademicRecordRepository;
  @Autowired private StudentCourseRepository studentCourseRepository;
  @Autowired private StudentDesignatedCourseRepository studentDesignatedCourseRepository;
  @Autowired private DepartmentRepository departmentRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseOfferingRepository courseOfferingRepository;
  @Autowired private ProfessorRepository professorRepository;
  @Autowired private CourseEvaluationRepository courseEvaluationRepository;
  @Autowired private CourseEvaluationTagRepository courseEvaluationTagRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private ReportRepository reportRepository;
  @SpyBean private StudentGraduationProgressRepository studentGraduationProgressRepository;

  @MockBean private AcademicCache academicCache;
  @MockBean private AuthTokenCache authTokenCache;

  @PersistenceContext private EntityManager entityManager;

  @Test
  @DisplayName("사용자가 탈퇴하면 User와 Student를 익명화하고 학적 정보만 삭제한다")
  void deleteUserAnonymizesUserAndStudentAndRemovesAcademicAssociations() {
    User user =
        userRepository.save(
            User.builder().email("test@haksa.com").profileNickname("tester").build());
    Student student = createStudent(user);
    final Report report =
        reportRepository.save(
            Report.create(
                user.getId(),
                "탈퇴 전 문의 제목",
                "탈퇴 전 문의 본문",
                new ReportSubmitterSnapshot(
                    user.getId(),
                    student.getDepartment().getId(),
                    student.getDepartment().getEstablishedDepartmentName(),
                    student.getStudentCode(),
                    student.getDepartment().getId(),
                    student.getDepartment().getEstablishedDepartmentName(),
                    null,
                    null,
                    false,
                    2024,
                    GraduationRequirementSnapshotStatus.AVAILABLE)));

    persistStudentAssociations(student);
    studentDesignatedCourseRepository.save(
        new StudentDesignatedCourse(student, designatedCourse("C101", 0)));

    entityManager.flush();
    entityManager.clear();

    userService.deleteUserById(user.getId());

    entityManager.flush();
    entityManager.clear();

    UUID studentId = student.getId();
    User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
    Student withdrawnStudent = studentRepository.findById(studentId).orElseThrow();
    assertThat(withdrawnUser.getIsDeleted()).isTrue();
    assertThat(withdrawnUser.getDeletedAt()).isNotNull();
    assertThat(withdrawnUser.getEmail()).isNull();
    assertThat(withdrawnUser.getProfileNickname()).isNull();
    assertThat(withdrawnUser.getProfileImage()).isNull();
    assertThat(withdrawnStudent.getName()).isEqualTo("탈퇴한 사용자입니다.");
    assertThat(withdrawnStudent.getStudentCode()).startsWith("deleted_");
    assertThat(studentAcademicRecordRepository.findByStudentId(studentId)).isEmpty();
    assertThat(semesterAcademicRecordRepository.findByStudentId(studentId)).isEmpty();
    assertThat(studentCourseRepository.findAll()).isEmpty();
    assertThat(studentGraduationProgressRepository.findByStudentId(studentId)).isEmpty();
    assertThat(studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(studentId))
        .isEmpty();
    Report anonymizedReport = reportRepository.findById(report.getId()).orElseThrow();
    assertThat(anonymizedReport.getUserId()).isEqualTo(user.getId());
    assertThat(anonymizedReport.getTitle()).isEqualTo("탈퇴 전 문의 제목");
    assertThat(anonymizedReport.getContent()).isEqualTo("탈퇴 전 문의 본문");
    assertThat(anonymizedReport.getSubmitterSnapshot().getSubmittedUserId()).isNull();
    assertThat(anonymizedReport.getSubmitterSnapshot().getStudentCode()).isNull();
    assertThat(anonymizedReport.getSubmitterSnapshot().getDepartmentName()).isNull();
    assertThat(anonymizedReport.getSubmitterSnapshot().getGraduationRequirementStatus())
        .isEqualTo(GraduationRequirementSnapshotStatus.UNKNOWN);

    verify(academicCache).deleteAllByStudentId(studentId);
    verify(authTokenCache).evictByUserId(user.getId().toString());
  }

  @Test
  @DisplayName("계정 병합 시 기존 학생의 지정과목과 학생 식별자를 유지한다")
  void mergePreservesStudentDesignatedCourses() {
    final User currentUser =
        userRepository.save(
            User.builder().email("merge-current@haksa.com").profileNickname("current").build());
    User existingUser =
        userRepository.save(
            User.builder().email("merge-existing@haksa.com").profileNickname("existing").build());
    Student existingStudent = createStudent(existingUser, "20263337");
    final Report existingReport =
        reportRepository.save(
            Report.create(
                existingUser.getId(),
                "병합 전 문의",
                "문의 내용",
                ReportSubmitterSnapshot.unknown(existingUser.getId())));
    studentDesignatedCourseRepository.save(
        new StudentDesignatedCourse(existingStudent, designatedCourse("C337", 0)));

    entityManager.flush();
    entityManager.clear();

    User merged = userService.tryMergeWithExistingUser(currentUser.getId(), "20263337");

    entityManager.flush();
    entityManager.clear();

    Student mergedStudent = studentRepository.findById(existingStudent.getId()).orElseThrow();
    assertThat(merged.getId()).isEqualTo(currentUser.getId());
    assertThat(mergedStudent.getUser().getId()).isEqualTo(currentUser.getId());
    assertThat(
            studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(
                mergedStudent.getId()))
        .extracting(StudentDesignatedCourse::getSubjtCd)
        .containsExactly("C337");
    assertThat(userRepository.findById(existingUser.getId())).isEmpty();
    Report reassignedReport = reportRepository.findById(existingReport.getId()).orElseThrow();
    assertThat(reassignedReport.getUserId()).isEqualTo(currentUser.getId());
    assertThat(reassignedReport.getSubmitterSnapshot().getSubmittedUserId())
        .isEqualTo(existingUser.getId());
  }

  @Test
  @DisplayName("연동하지 않은 사용자의 탈퇴에서는 User만 익명화한다")
  void deleteUserWithoutStudentDoesNotFail() {
    User user =
        userRepository.save(
            User.builder().email("orphan@haksa.com").profileNickname("orphan").build());

    entityManager.flush();
    entityManager.clear();

    userService.deleteUserById(user.getId());

    entityManager.flush();
    entityManager.clear();

    User withdrawnUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(withdrawnUser.getIsDeleted()).isTrue();
    assertThat(withdrawnUser.getEmail()).isNull();
    verify(academicCache, never()).deleteAllByStudentId(any());
    verify(authTokenCache).evictByUserId(user.getId().toString());
    verify(studentRepository, never()).delete(any());
    verify(studentAcademicRecordRepository, never()).deleteByStudentId(any());
    verify(studentGraduationProgressRepository, never()).deleteByStudentId(any());
  }

  @Test
  @DisplayName("강의평가가 있는 회원도 탈퇴할 수 있고 재가입 학생과 과거 평가는 분리된다")
  void deleteUserPreservesCourseEvaluationAndAllowsNewStudentWithOriginalStudentCode() {
    String studentCode = "20260002";
    User user =
        userRepository.save(
            User.builder().email("evaluation@haksa.com").profileNickname("evaluation").build());
    Student student = createStudent(user, studentCode);
    Course course = courseRepository.save(new Course("CS102", "알고리즘"));
    Professor professor = professorRepository.save(new Professor("홍길동"));
    final CourseEvaluation evaluation =
        courseEvaluationRepository.save(
            new CourseEvaluation(
                student,
                course,
                professor,
                2026,
                1,
                "강의평가",
                List.of(LectureEvaluationTag.INFORMATIVE_LECTURE)));
    socialAccountRepository.save(
        SocialAccount.builder()
            .provider(OidcProvider.KAKAO)
            .socialId("withdrawn-social-id")
            .email("evaluation@haksa.com")
            .user(user)
            .build());
    refreshTokenRepository.save(
        new RefreshToken(
            "withdrawn-session",
            user.getId().toString(),
            "refresh-token",
            new Date(System.currentTimeMillis() + 60_000)));

    entityManager.flush();
    entityManager.clear();

    userService.deleteUserById(user.getId());

    entityManager.flush();
    entityManager.clear();

    Student withdrawnStudent = studentRepository.findById(student.getId()).orElseThrow();
    assertThat(withdrawnStudent.getStudentCode()).startsWith("deleted_");
    assertThat(courseEvaluationRepository.findById(evaluation.getId())).isPresent();
    assertThat(courseEvaluationTagRepository.count()).isEqualTo(1);
    assertThat(
            socialAccountRepository.findByProviderAndSocialId(
                OidcProvider.KAKAO, "withdrawn-social-id"))
        .isEmpty();
    assertThat(refreshTokenRepository.findAll())
        .noneMatch(token -> token.getUserId().equals(user.getId().toString()));

    User rejoinedUser =
        userRepository.save(
            User.builder().email("rejoined@haksa.com").profileNickname("rejoined").build());
    Student rejoinedStudent = createStudent(rejoinedUser, studentCode);

    entityManager.flush();
    entityManager.clear();

    assertThat(rejoinedStudent.getId()).isNotEqualTo(student.getId());
    assertThat(userRepository.findByStudentStudentCode(studentCode))
        .map(User::getId)
        .contains(rejoinedUser.getId());
    assertThat(
            courseEvaluationRepository
                .findById(evaluation.getId())
                .orElseThrow()
                .getStudent()
                .getId())
        .isEqualTo(student.getId());
  }

  @Test
  @DisplayName("사용자와 소셜 계정은 이메일 없이 저장할 수 있다")
  void nullableSocialEmailIsPersistedAsNull() {
    User user =
        userRepository.save(User.builder().email(null).profileNickname("email-less").build());
    socialAccountRepository.save(
        SocialAccount.builder()
            .provider(OidcProvider.APPLE)
            .socialId("email-less-sub")
            .email(null)
            .user(user)
            .build());

    entityManager.flush();
    entityManager.clear();

    SocialAccount account =
        socialAccountRepository
            .findByProviderAndSocialId(OidcProvider.APPLE, "email-less-sub")
            .orElseThrow();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isNull();
    assertThat(account.getEmail()).isNull();
  }

  private Student createStudent(User user) {
    return createStudent(user, "20260001");
  }

  private Student createStudent(User user, String studentCode) {
    Department department =
        departmentRepository
            .findByDepartmentCode("2000513")
            .orElseGet(() -> departmentRepository.save(new Department("2000513", "컴퓨터학과")));
    Student student =
        Student.builder()
            .studentCode(studentCode)
            .name("학생")
            .department(department)
            .major(null)
            .secondaryMajor(null)
            .admissionYear(2024)
            .semesterEnrolled(1)
            .isTransferStudent(false)
            .isGraduated(false)
            .status(StudentStatus.재학)
            .gradeLevel(1)
            .completedSemesters(0)
            .admissionType("수시")
            .user(user)
            .build();
    return studentRepository.save(student);
  }

  private void persistStudentAssociations(Student student) {
    studentAcademicRecordRepository.save(
        new StudentAcademicRecord(
            student, 30, 24, BigDecimal.valueOf(3.8), BigDecimal.valueOf(85)));

    SemesterAcademicRecord semesterRecord =
        new SemesterAcademicRecord(
            student,
            2024,
            1,
            15,
            15,
            BigDecimal.valueOf(3.9),
            BigDecimal.valueOf(88),
            BigDecimal.valueOf(3.9),
            1,
            30);
    student.addSemesterRecord(semesterRecord);
    semesterAcademicRecordRepository.save(semesterRecord);

    Course course = courseRepository.save(new Course("CS101", "자료구조"));
    CourseOffering offering =
        courseOfferingRepository.save(
            new CourseOffering(
                1,
                false,
                2024,
                1,
                "공과대학",
                "A",
                "월1",
                null,
                3,
                EvaluationType.ABSOLUTE,
                FacultyDivision.전선,
                course,
                null,
                student.getDepartment(),
                null));

    StudentCourse studentCourse =
        new StudentCourse(student, offering, new Grade(GradeType.A0), 3, false, 95, false);
    student.addStudentCourse(studentCourse);
    studentCourseRepository.save(studentCourse);

    studentGraduationProgressRepository.save(
        StudentGraduationProgress.createForLanguageCert(student, true));
  }

  private static DesignatedCourseData designatedCourse(String code, int sourceOrder) {
    return new DesignatedCourseData(
        "01", code, "지정과목", 3, "TRANSFER", 2024, "1학기", "", sourceOrder);
  }
}
