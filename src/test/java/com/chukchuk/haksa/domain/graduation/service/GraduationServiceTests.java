package com.chukchuk.haksa.domain.graduation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.graduation.policy.MajorResolutionResult;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class GraduationServiceTests {

  @Mock private StudentService studentService;
  @Mock private GraduationQueryRepository graduationQueryRepository;
  @Mock private AcademicCache academicCache;
  @Mock private GraduationMajorResolver graduationMajorResolver;
  @Mock private StudentGraduationProgressService studentGraduationProgressService;
  @Mock private TransferGraduationAnalysisService transferGraduationAnalysisService;

  @InjectMocks private GraduationService graduationService;

  private static final UUID STUDENT_ID = UUID.randomUUID();
  private static final int ADMISSION_YEAR = 2022;

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  @DisplayName("전공 학과가 존재하면 전공 학과 ID로 졸업요건을 조회한다")
  void getGraduationProgressUsesMajorDepartmentIdFirst() {
    Student student = mockStudent(10L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));

    List<AreaProgressDto> progressDtos = sampleProgress();
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(progressDtos);

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getGraduationProgress()).isEqualTo(progressDtos);
    verify(graduationQueryRepository).getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR);
    verify(academicCache)
        .setGraduationProgress(eq(STUDENT_ID), any(GraduationProgressResponse.class));
  }

  @Test
  @DisplayName("resolver가 반환한 학과 ID로 단일전공 조회가 실패하면 예외를 던진다")
  void getGraduationProgressThrowsWhenSingleMajorProgressEmpty() {
    Student student = mockStudent(1L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(1L, null));

    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 1L, ADMISSION_YEAR))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> graduationService.getGraduationProgress(STUDENT_ID))
        .isInstanceOf(CommonException.class)
        .hasMessage(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.message());

    assertGraduationContext("1", "NONE", "SINGLE");
  }

  @Test
  @DisplayName("복수전공 진행률 조회가 실패하면 최종 전공 문맥을 남긴다")
  void getGraduationProgressAddsContextWhenDualMajorProgressMissing() {
    Student student = mockStudent(60L, 71L);
    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(60L, 71L));
    when(graduationQueryRepository.getDualMajorAreaProgress(STUDENT_ID, 60L, 71L, ADMISSION_YEAR))
        .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));

    assertThatThrownBy(() -> graduationService.getGraduationProgress(STUDENT_ID))
        .isInstanceOf(CommonException.class)
        .hasMessage(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.message());

    assertGraduationContext("60", "71", "DUAL");
  }

  @Test
  @DisplayName("resolver가 예외를 던지면 그대로 전파한다")
  void getGraduationProgressPropagatesResolverException() {
    Student student = mockStudent(5L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenThrow(new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND));

    assertThatThrownBy(() -> graduationService.getGraduationProgress(STUDENT_ID))
        .isInstanceOf(CommonException.class)
        .hasMessage(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.message());
  }

  @Test
  @DisplayName("복수전공 학과 후보들을 순차적으로 시도하여 졸업요건을 찾는다")
  void getGraduationProgressFallbacksSecondaryMajorCandidates() {
    Student student = mockStudent(60L, 71L);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(60L, 71L));

    List<AreaProgressDto> dualProgress = sampleProgress();
    when(graduationQueryRepository.getDualMajorAreaProgress(STUDENT_ID, 60L, 71L, ADMISSION_YEAR))
        .thenReturn(dualProgress);

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getGraduationProgress()).isEqualTo(dualProgress);
    verify(graduationQueryRepository)
        .getDualMajorAreaProgress(STUDENT_ID, 60L, 71L, ADMISSION_YEAR);
  }

  @Test
  @DisplayName("캐시에 데이터가 있으면 resolver와 repository를 호출하지 않는다")
  void returnsCachedProgressWhenAvailable() {
    Student student = mockStudent(10L, null, ADMISSION_YEAR, false);
    GraduationProgressResponse cached = new GraduationProgressResponse(sampleProgress());

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(cached);

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response).isEqualTo(cached);
    verifyNoInteractions(graduationMajorResolver, graduationQueryRepository);
  }

  @Test
  @DisplayName("편입생은 일반 졸업진단 캐시를 우회하고 전용 분석을 사용한다")
  void usesDedicatedAnalysisForTransferStudentBeforeCache() {
    Student student = mockStudent(10L, null, ADMISSION_YEAR, true);
    GraduationProgressResponse transferResponse = new GraduationProgressResponse(List.of());

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(transferGraduationAnalysisService.analyze(student)).thenReturn(transferResponse);

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response).isSameAs(transferResponse);
    verify(transferGraduationAnalysisService).analyze(student);
    verifyNoInteractions(academicCache);
    verifyNoInteractions(graduationMajorResolver, graduationQueryRepository);
  }

  @Test
  @DisplayName("입학년도 2025 + 특정 학과는 특이 졸업요건 플래그를 true로 반환한다")
  void marksDifferentGraduationRequirementForSpecialCase() {
    int specialYear = 2025;
    Student student = mockStudent(30L, null, specialYear, false);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, specialYear))
        .thenReturn(new MajorResolutionResult(30L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 30L, specialYear))
        .thenReturn(sampleProgress());

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.isHasDifferentGraduationRequirement()).isTrue();
  }

  @Test
  @DisplayName("캐시 조회 예외가 나도 DB 조회로 정상 응답한다")
  void getGraduationProgressCacheGetFailureFallsBack() {
    Student student = mockStudent(10L, null);
    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID))
        .thenThrow(new RuntimeException("cache get fail"));
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(sampleProgress());

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getGraduationProgress()).isNotEmpty();
  }

  @Test
  @DisplayName("캐시 저장 예외가 나도 최종 응답은 정상 반환한다")
  void getGraduationProgressCacheSetFailureStillReturns() {
    Student student = mockStudent(10L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(sampleProgress());
    doThrow(new RuntimeException("cache set fail"))
        .when(academicCache)
        .setGraduationProgress(eq(STUDENT_ID), any(GraduationProgressResponse.class));

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getGraduationProgress()).hasSize(1);
  }

  @Test
  @DisplayName("외국어 인증 값이 없으면 새로고침 필요 상태로 반환한다")
  void getGraduationProgressReturnsRefreshNeededWhenLanguageCertMissing() {
    Student student = mockStudent(10L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(sampleProgress());
    when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.empty());

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getLanguageCertFulfilled()).isNull();
    assertThat(response.isLanguageCertNeedsRefresh()).isTrue();
  }

  @Test
  @DisplayName("외국어 인증 통과 값이 있으면 통과와 새로고침 불필요 상태를 반환한다")
  void getGraduationProgressReturnsLanguageCertFulfilled() {
    Student student = mockStudent(10L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(sampleProgress());
    when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.of(true));

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getLanguageCertFulfilled()).isTrue();
    assertThat(response.isLanguageCertNeedsRefresh()).isFalse();
  }

  @Test
  @DisplayName("외국어 인증 미통과 값이 있으면 미통과와 새로고침 불필요 상태를 반환한다")
  void getGraduationProgressReturnsLanguageCertNotFulfilled() {
    Student student = mockStudent(10L, null);

    when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
    when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
    when(graduationMajorResolver.resolve(student, ADMISSION_YEAR))
        .thenReturn(new MajorResolutionResult(10L, null));
    when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
        .thenReturn(sampleProgress());
    when(studentGraduationProgressService.getLanguageCertFulfilled(STUDENT_ID))
        .thenReturn(Optional.of(false));

    GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

    assertThat(response.getLanguageCertFulfilled()).isFalse();
    assertThat(response.isLanguageCertNeedsRefresh()).isFalse();
  }

  private Student mockStudent(Long majorId, Long secondaryId) {
    return mockStudent(majorId, secondaryId, ADMISSION_YEAR, false);
  }

  private Student mockStudent(
      Long majorId, Long secondaryId, int admissionYear, boolean transferStudent) {
    Student student = mock(Student.class);
    lenient().when(student.isTransferStudent()).thenReturn(transferStudent);
    lenient().when(student.getStudentCode()).thenReturn("20221234");

    AcademicInfo info =
        AcademicInfo.builder()
            .admissionYear(admissionYear)
            .isTransferStudent(transferStudent)
            .build();
    lenient().when(student.getAcademicInfo()).thenReturn(info);

    Department primary = mock(Department.class);
    lenient().when(primary.getId()).thenReturn(majorId);
    lenient().when(student.getMajor()).thenReturn(primary);

    Department department = mock(Department.class);
    lenient().when(department.getId()).thenReturn(majorId);
    lenient().when(student.getDepartment()).thenReturn(department);

    if (secondaryId != null) {
      Department secondary = mock(Department.class);
      lenient().when(secondary.getId()).thenReturn(secondaryId);
      lenient().when(student.getSecondaryMajor()).thenReturn(secondary);
    } else {
      lenient().when(student.getSecondaryMajor()).thenReturn(null);
    }

    return student;
  }

  private void assertGraduationContext(
      String departmentId, String secondaryDepartmentId, String majorType) {
    assertThat(MDC.get("studentCodeHash")).isEqualTo("LUJPiDE6PY");
    assertThat(MDC.get("admissionYear")).isEqualTo("2022");
    assertThat(MDC.get("departmentId")).isEqualTo(departmentId);
    assertThat(MDC.get("secondaryDepartmentId")).isEqualTo(secondaryDepartmentId);
    assertThat(MDC.get("majorType")).isEqualTo(majorType);
  }

  private List<AreaProgressDto> sampleProgress() {
    CourseDto course = new CourseDto(2022, "Data Structures", 3, "A+", 1, null);
    AreaProgressDto dto = new AreaProgressDto(FacultyDivision.전핵, 12, 6, 1, 1, 2, List.of(course));
    return List.of(dto);
  }
}
