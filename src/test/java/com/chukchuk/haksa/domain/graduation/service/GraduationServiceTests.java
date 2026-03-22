package com.chukchuk.haksa.domain.graduation.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GraduationServiceTests {

    @Mock
    private StudentService studentService;
    @Mock
    private GraduationQueryRepository graduationQueryRepository;
    @Mock
    private AcademicCache academicCache;
    @Mock
    private GraduationMajorResolver graduationMajorResolver;

    @InjectMocks
    private GraduationService graduationService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final int ADMISSION_YEAR = 2022;
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
        verify(academicCache).setGraduationProgress(eq(STUDENT_ID), any(GraduationProgressResponse.class));
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
        verify(graduationQueryRepository).getDualMajorAreaProgress(STUDENT_ID, 60L, 71L, ADMISSION_YEAR);
    }

    @Test
    @DisplayName("캐시에 데이터가 있으면 resolver와 repository를 호출하지 않는다")
    void returnsCachedProgressWhenAvailable() {
        GraduationProgressResponse cached = new GraduationProgressResponse(sampleProgress());

        when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(cached);

        GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

        assertThat(response).isEqualTo(cached);
        verifyNoInteractions(studentService, graduationMajorResolver, graduationQueryRepository);
    }

    @Test
    @DisplayName("편입생이면 TRANSFER_STUDENT_UNSUPPORTED 예외를 던진다")
    void getGraduationProgressThrowsForTransferStudent() {
        Student student = mockStudent(10L, null, ADMISSION_YEAR, true);
        when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);

        assertThatThrownBy(() -> graduationService.getGraduationProgress(STUDENT_ID))
                .isInstanceOf(CommonException.class)
                .hasMessage(ErrorCode.TRANSFER_STUDENT_UNSUPPORTED.message());

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
        when(academicCache.getGraduationProgress(STUDENT_ID)).thenThrow(new RuntimeException("cache get fail"));
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
                .when(academicCache).setGraduationProgress(eq(STUDENT_ID), any(GraduationProgressResponse.class));

        GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

        assertThat(response.getGraduationProgress()).hasSize(1);
    }

    private Student mockStudent(Long majorId, Long secondaryId) {
        return mockStudent(majorId, secondaryId, ADMISSION_YEAR, false);
    }

    private Student mockStudent(Long majorId, Long secondaryId, int admissionYear, boolean transferStudent) {
        Student student = mock(Student.class);
        lenient().when(student.isTransferStudent()).thenReturn(transferStudent);

        AcademicInfo info = AcademicInfo.builder()
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

    private List<AreaProgressDto> sampleProgress() {
        CourseDto course = new CourseDto(2022, "Data Structures", 3, "A+", 1);
        AreaProgressDto dto = new AreaProgressDto(
                FacultyDivision.전핵,
                12,
                6,
                1,
                1,
                2,
                List.of(course)
        );
        return List.of(dto);
    }

}
