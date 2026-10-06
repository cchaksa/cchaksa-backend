// 편입생 졸업요건 부분 진단 서비스의 계산 계약을 검증한다.

package com.chukchuk.haksa.domain.graduation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.academic.record.service.StudentAcademicRecordService;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseCompletionStatus;
import com.chukchuk.haksa.domain.graduation.dto.DesignatedCourseProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.dto.TransferAreaEvaluationType;
import com.chukchuk.haksa.domain.graduation.dto.TransferGraduationProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.TransferManualReviewReason;
import com.chukchuk.haksa.domain.graduation.policy.DesignatedCourseEvaluator;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.graduation.policy.TransferAreaEvaluator;
import com.chukchuk.haksa.domain.graduation.policy.TransferCourseEvaluator;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferGraduationAnalysisServiceTests {

  private static final UUID STUDENT_ID = UUID.randomUUID();

  @Mock private StudentAcademicRecordService studentAcademicRecordService;
  @Mock private StudentCourseRepository studentCourseRepository;
  @Mock private StudentDesignatedCourseRepository studentDesignatedCourseRepository;
  @Mock private StudentGraduationProgressService studentGraduationProgressService;
  @Mock private DesignatedCourseEvaluator designatedCourseEvaluator;

  @Mock private TransferCourseEvaluator transferCourseEvaluator;

  @Mock private TransferAreaEvaluator transferAreaEvaluator;

  @Mock private GraduationMajorResolver graduationMajorResolver;
  @Mock private GraduationQueryRepository graduationQueryRepository;

  @InjectMocks private TransferGraduationAnalysisService service;

  private Student student;
  private StudentAcademicRecord academicRecord;

  @BeforeEach
  void setUp() {
    student = mock(Student.class);
    academicRecord = mock(StudentAcademicRecord.class);
    AcademicInfo academicInfo =
        AcademicInfo.builder().completedSemesters(3).isTransferStudent(true).build();

    lenient().when(student.getId()).thenReturn(STUDENT_ID);
    lenient().when(student.getAcademicInfo()).thenReturn(academicInfo);
    lenient().when(student.getSecondaryMajor()).thenReturn(null);
    lenient()
        .when(studentAcademicRecordService.findStudentAcademicRecordByStudentId(STUDENT_ID))
        .thenReturn(Optional.of(academicRecord));
    lenient()
        .when(studentCourseRepository.findAllWithCourseByStudentId(STUDENT_ID))
        .thenReturn(List.of());
    lenient()
        .when(studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(STUDENT_ID))
        .thenReturn(List.of());
    TransferCourseEvaluator.Evaluation emptyCourseEvaluation =
        new TransferCourseEvaluator.Evaluation(List.of(), Map.of(), Map.of(), Set.of(), 0);
    lenient().when(transferCourseEvaluator.evaluate(anyList())).thenReturn(emptyCourseEvaluation);
    lenient().when(transferAreaEvaluator.evaluate(any(), any())).thenReturn(List.of());
    lenient()
        .when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.of(true));
  }

  @Test
  void calculatesTransferCreditsAndGpaFromCumulativeAcademicRecord() {
    when(academicRecord.getTotalEarnedCredits()).thenReturn(112);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("3.2"));

    GraduationProgressResponse response = service.analyze(student);
    TransferGraduationProgressDto progress = response.getTransferProgress();

    assertThat(progress.requiredTotalCredits()).isEqualTo(130);
    assertThat(progress.totalEarnedCredits()).isEqualTo(112);
    assertThat(progress.remainingCredits()).isEqualTo(18);
    assertThat(progress.creditsFulfilled()).isFalse();
    assertThat(progress.recognizedTransferCredits()).isZero();
    assertThat(progress.cumulativeGpa()).isEqualByComparingTo("3.2");
    assertThat(progress.requiredGpa()).isEqualByComparingTo("2.0");
    assertThat(progress.gpaFulfilled()).isTrue();
    assertThat(progress.completedSemesters()).isEqualTo(3);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"NULL,NULL", "0,3", "3,6"},
      nullValues = "NULL")
  void separatesDesignatedCompletionFromPersonalCreditAvailability(
      Integer personalCredits, Integer expectedCredits) {
    TransferGraduationAnalysisService realEvaluatorService =
        new TransferGraduationAnalysisService(
            studentAcademicRecordService,
            studentCourseRepository,
            studentDesignatedCourseRepository,
            studentGraduationProgressService,
            new DesignatedCourseEvaluator(),
            new TransferCourseEvaluator(),
            new TransferAreaEvaluator(),
            graduationMajorResolver,
            graduationQueryRepository);
    when(student.getDesignatedCoursesSnapshotVersion())
        .thenReturn(Instant.parse("2026-09-02T00:00:00Z"));
    when(academicRecord.getTotalEarnedCredits()).thenReturn(112);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("3.2"));
    when(studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(STUDENT_ID))
        .thenReturn(
            List.of(
                designatedCourse("C101", 0),
                designatedCourse("C102", 1),
                designatedCourse("C103", 2)));
    when(studentCourseRepository.findAllWithCourseByStudentId(STUDENT_ID))
        .thenReturn(List.of(completedCourse("C101", personalCredits), completedCourse("C102", 3)));

    TransferGraduationProgressDto progress =
        realEvaluatorService.analyze(student).getTransferProgress();

    assertThat(progress.designatedCourses())
        .extracting(DesignatedCourseProgressDto::status)
        .containsExactly(
            DesignatedCourseCompletionStatus.COMPLETED,
            DesignatedCourseCompletionStatus.COMPLETED,
            DesignatedCourseCompletionStatus.NOT_COMPLETED);
    assertThat(progress.designatedEarnedCredits()).isEqualTo(expectedCredits);
    assertThat(progress.designatedCreditUnavailableReasons())
        .isEqualTo(expectedCredits == null ? List.of("COURSE_DATA_INCOMPLETE") : List.of());
    assertThat(progress.totalEarnedCredits()).isEqualTo(112);
  }

  private StudentDesignatedCourse designatedCourse(String code, int sourceOrder) {
    return new StudentDesignatedCourse(
        student,
        new DesignatedCourseData(null, code, code, 3, null, null, null, null, sourceOrder));
  }

  private StudentCourse completedCourse(String code, Integer credits) {
    CourseOffering offering =
        new CourseOffering(
            null,
            false,
            2025,
            1,
            null,
            null,
            null,
            null,
            3,
            null,
            FacultyDivision.전핵,
            new Course(code, code),
            null,
            null,
            null);
    return new StudentCourse(
        student, offering, new Grade(GradeType.P), credits, false, null, false);
  }

  @Test
  void appliesHigherGpaRequirementWhenSecondaryMajorExists() {
    Department secondaryMajor = mock(Department.class);
    when(student.getSecondaryMajor()).thenReturn(secondaryMajor);
    when(academicRecord.getTotalEarnedCredits()).thenReturn(130);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("2.3"));

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.remainingCredits()).isZero();
    assertThat(progress.creditsFulfilled()).isTrue();
    assertThat(progress.requiredGpa()).isEqualByComparingTo("2.5");
    assertThat(progress.gpaFulfilled()).isFalse();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, 1, 2})
  void preservesEarnedProgressWhenTransferYearIsMissing(Integer year) {
    when(student.getAcademicInfo()).thenReturn(AcademicInfo.builder().admissionYear(year).build());

    TransferGraduationProgressDto progress =
        withRealEvaluators().analyze(student).getTransferProgress();

    assertThat(progress.areas())
        .extracting(area -> area.evaluationType())
        .containsExactly(
            TransferAreaEvaluationType.UNAVAILABLE, TransferAreaEvaluationType.UNAVAILABLE);
    verifyNoInteractions(graduationMajorResolver, graduationQueryRepository);
  }

  @Test
  void preservesProgressWhenDepartmentIsMissing() {
    when(student.getAcademicInfo()).thenReturn(AcademicInfo.builder().admissionYear(2026).build());

    assertThat(withRealEvaluators().analyze(student).getTransferProgress().areas())
        .allSatisfy(
            area ->
                assertThat(area.evaluationType())
                    .isEqualTo(TransferAreaEvaluationType.UNAVAILABLE));
    verifyNoInteractions(graduationMajorResolver, graduationQueryRepository);
  }

  @Test
  void doesNotApplySingleMajorHalfRuleToSecondaryMajor() {
    when(student.getAcademicInfo()).thenReturn(AcademicInfo.builder().admissionYear(2026).build());
    when(student.getDepartment()).thenReturn(mock(Department.class));
    when(student.getSecondaryMajor()).thenReturn(mock(Department.class));

    assertThat(withRealEvaluators().analyze(student).getTransferProgress().areas())
        .allSatisfy(
            area ->
                assertThat(area.evaluationType())
                    .isEqualTo(TransferAreaEvaluationType.UNAVAILABLE));
    verifyNoInteractions(graduationMajorResolver, graduationQueryRepository);
  }

  @Test
  void preservesPartialProgressWhenCohortRequirementsAreNotFound() {
    when(student.getAcademicInfo()).thenReturn(AcademicInfo.builder().admissionYear(2026).build());
    when(student.getDepartment()).thenReturn(mock(Department.class));
    when(graduationMajorResolver.resolve(student, 2024))
        .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));

    assertThat(withRealEvaluators().analyze(student).getTransferProgress().areas())
        .allSatisfy(
            area ->
                assertThat(area.evaluationType())
                    .isEqualTo(TransferAreaEvaluationType.UNAVAILABLE));
  }

  @Test
  void doesNotHideOtherRequirementLookupFailures() {
    when(student.getAcademicInfo()).thenReturn(AcademicInfo.builder().admissionYear(2026).build());
    when(student.getDepartment()).thenReturn(mock(Department.class));
    IllegalStateException failure = new IllegalStateException("test lookup failure");
    when(graduationMajorResolver.resolve(student, 2024)).thenThrow(failure);

    assertThatThrownBy(() -> withRealEvaluators().analyze(student)).isSameAs(failure);
  }

  private TransferGraduationAnalysisService withRealEvaluators() {
    return new TransferGraduationAnalysisService(
        studentAcademicRecordService,
        studentCourseRepository,
        studentDesignatedCourseRepository,
        studentGraduationProgressService,
        new DesignatedCourseEvaluator(),
        new TransferCourseEvaluator(),
        new TransferAreaEvaluator(),
        graduationMajorResolver,
        graduationQueryRepository);
  }

  @Test
  void exposesDesignatedCourseStatusesAndRefreshState() {
    StudentDesignatedCourse designated = mock(StudentDesignatedCourse.class);
    StudentCourse completedCourse = mock(StudentCourse.class);
    List<StudentDesignatedCourse> designatedCourses = List.of(designated);
    List<StudentCourse> studentCourses = List.of(completedCourse);
    when(studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(STUDENT_ID))
        .thenReturn(designatedCourses);
    lenient()
        .when(
            designatedCourseEvaluator.evaluate(
                anyList(), any(TransferCourseEvaluator.Evaluation.class)))
        .thenReturn(null);
    when(studentCourseRepository.findAllWithCourseByStudentId(STUDENT_ID))
        .thenReturn(studentCourses);
    TransferCourseEvaluator.Evaluation courseEvaluation =
        new TransferCourseEvaluator.Evaluation(
            List.of(), Map.of("C101", 3), Map.of("C101", 3), Set.of(), 65);
    when(transferCourseEvaluator.evaluate(studentCourses)).thenReturn(courseEvaluation);
    when(designatedCourseEvaluator.evaluate(designatedCourses, courseEvaluation))
        .thenReturn(
            new DesignatedCourseEvaluator.Evaluation(
                List.of(
                    new DesignatedCourseProgressDto(
                        "C101", "자료구조", 3, DesignatedCourseCompletionStatus.COMPLETED)),
                65));
    when(student.getDesignatedCoursesSnapshotVersion()).thenReturn((Instant) null);
    when(academicRecord.getTotalEarnedCredits()).thenReturn(112);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("3.2"));

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.recognizedTransferCredits()).isEqualTo(65);
    assertThat(progress.designatedCourses())
        .extracting(DesignatedCourseProgressDto::status)
        .containsExactly(DesignatedCourseCompletionStatus.COMPLETED);
    assertThat(progress.designatedCoursesNeedsRefresh()).isTrue();
    assertThat(progress.designatedEarnedCredits()).isNull();
    assertThat(progress.designatedCreditUnavailableReasons())
        .containsExactly("SNAPSHOT_NOT_RECEIVED");
    assertThat(progress.manualReviewRequired()).isTrue();
    assertThat(progress.manualReviewReasons())
        .contains(TransferManualReviewReason.GRADUATION_REVIEW_NOT_AVAILABLE);
  }

  @Test
  void treatsReceivedEmptyDesignatedSnapshotAsFresh() {
    when(student.getDesignatedCoursesSnapshotVersion())
        .thenReturn(Instant.parse("2026-09-02T00:00:00Z"));
    when(academicRecord.getTotalEarnedCredits()).thenReturn(130);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("2.0"));

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.designatedCourses()).isEmpty();
    assertThat(progress.designatedCoursesNeedsRefresh()).isFalse();
    assertThat(progress.designatedEarnedCredits()).isZero();
    assertThat(progress.designatedCreditUnavailableReasons()).isEmpty();
  }

  @Test
  void keepsDesignatedCreditUnknownWhenDesignatedCodeIsMissing() {
    StudentDesignatedCourse designated = mock(StudentDesignatedCourse.class);
    List<StudentDesignatedCourse> designatedCourses = List.of(designated);
    when(studentDesignatedCourseRepository.findAllByStudentIdOrderBySourceOrder(STUDENT_ID))
        .thenReturn(designatedCourses);
    lenient()
        .when(
            designatedCourseEvaluator.evaluate(
                anyList(), any(TransferCourseEvaluator.Evaluation.class)))
        .thenReturn(null);
    when(designatedCourseEvaluator.evaluate(designatedCourses, List.of()))
        .thenReturn(
            new DesignatedCourseEvaluator.Evaluation(
                List.of(
                    new DesignatedCourseProgressDto(
                        null, "자료구조", 3, DesignatedCourseCompletionStatus.UNKNOWN)),
                0));
    when(student.getDesignatedCoursesSnapshotVersion())
        .thenReturn(Instant.parse("2026-09-02T00:00:00Z"));

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.designatedEarnedCredits()).isNull();
    assertThat(progress.designatedCreditUnavailableReasons())
        .containsExactly("COURSE_DATA_INCOMPLETE");
  }

  @Test
  void returnsNullFulfillmentAndManualReasonWhenAcademicSummaryIsIncomplete() {
    when(academicRecord.getTotalEarnedCredits()).thenReturn(null);
    when(academicRecord.getCumulativeGpa()).thenReturn(null);

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.totalEarnedCredits()).isNull();
    assertThat(progress.remainingCredits()).isNull();
    assertThat(progress.creditsFulfilled()).isNull();
    assertThat(progress.gpaFulfilled()).isNull();
    assertThat(progress.manualReviewReasons())
        .contains(TransferManualReviewReason.ACADEMIC_SUMMARY_INCOMPLETE);
  }

  @Test
  void returnsPartialDiagnosisWhenAcademicSummaryRecordIsMissing() {
    when(studentAcademicRecordService.findStudentAcademicRecordByStudentId(STUDENT_ID))
        .thenReturn(Optional.empty());

    TransferGraduationProgressDto progress = service.analyze(student).getTransferProgress();

    assertThat(progress.totalEarnedCredits()).isNull();
    assertThat(progress.remainingCredits()).isNull();
    assertThat(progress.creditsFulfilled()).isNull();
    assertThat(progress.cumulativeGpa()).isNull();
    assertThat(progress.gpaFulfilled()).isNull();
    assertThat(progress.manualReviewReasons())
        .contains(TransferManualReviewReason.ACADEMIC_SUMMARY_INCOMPLETE);
  }

  @Test
  void preservesLanguageCertificateContract() {
    when(academicRecord.getTotalEarnedCredits()).thenReturn(130);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("2.0"));
    when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.empty());

    GraduationProgressResponse response = service.analyze(student);

    assertThat(response.getLanguageCertFulfilled()).isNull();
    assertThat(response.isLanguageCertNeedsRefresh()).isTrue();
  }

  @Test
  void keepsPartialDiagnosisWithoutFinalEligibility() {
    when(student.getAcademicInfo())
        .thenReturn(
            AcademicInfo.builder()
                .admissionYear(2024)
                .completedSemesters(4)
                .isTransferStudent(true)
                .build());
    when(student.getDesignatedCoursesSnapshotVersion())
        .thenReturn(Instant.parse("2026-09-02T00:00:00Z"));
    when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.of(true));
    when(academicRecord.getTotalEarnedCredits()).thenReturn(130);
    when(academicRecord.getCumulativeGpa()).thenReturn(new BigDecimal("3.2"));
    GraduationProgressResponse response = service.analyze(student);
    TransferGraduationProgressDto progress = response.getTransferProgress();

    assertThat(response.getAnalysisStatus())
        .isEqualTo(
            com.chukchuk.haksa.domain.graduation.dto.GraduationAnalysisStatus
                .MANUAL_REVIEW_REQUIRED);
    assertThat(progress.areas()).isEmpty();
  }
}
