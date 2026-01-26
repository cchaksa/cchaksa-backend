package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private GraduationService graduationService;

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final int ADMISSION_YEAR = 2022;
    private static final String DEPARTMENT_NAME = "Global Business";

    @Test
    @DisplayName("전공 학과가 존재하면 전공 학과 ID로 졸업요건을 조회한다")
    void getGraduationProgressUsesMajorDepartmentIdFirst() {
        Department department = mockDepartment(20L, "Another Name");
        Department major = mockDepartment(10L, DEPARTMENT_NAME);
        Student student = mockStudent(department, major);

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
        List<Department> siblings = List.of(mockDepartment(30L, DEPARTMENT_NAME));
        when(departmentRepository.findAllByEstablishedDepartmentName(DEPARTMENT_NAME))
                .thenReturn(siblings);

        when(graduationQueryRepository.getAreaRequirementsWithCache(10L, ADMISSION_YEAR))
                .thenReturn(sampleRequirements());

        List<AreaProgressDto> progressDtos = sampleProgress();
        when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR))
                .thenReturn(progressDtos);

        GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

        assertThat(response.getGraduationProgress()).isEqualTo(progressDtos);
        verify(graduationQueryRepository).getStudentAreaProgress(STUDENT_ID, 10L, ADMISSION_YEAR);
        verify(graduationQueryRepository, never()).getStudentAreaProgress(STUDENT_ID, 30L, ADMISSION_YEAR);
        verify(academicCache).setGraduationProgress(eq(STUDENT_ID), any(GraduationProgressResponse.class));
        verify(departmentRepository).findAllByEstablishedDepartmentName(DEPARTMENT_NAME);
        verify(departmentRepository, never()).findAllByEstablishedDepartmentName("Another Name");
    }

    @Test
    @DisplayName("첫 학과에 졸업요건이 없으면 다음 학과로 폴백한다")
    void getGraduationProgressTriesNextDepartmentWhenFirstMisses() {
        Department department = mockDepartment(1L, DEPARTMENT_NAME);
        Student student = mockStudent(department, null);

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
        Department candidate1 = mockDepartment(1L, DEPARTMENT_NAME);
        Department candidate2 = mockDepartment(2L, DEPARTMENT_NAME);
        when(departmentRepository.findAllByEstablishedDepartmentName(DEPARTMENT_NAME))
                .thenReturn(List.of(candidate1, candidate2));

        when(graduationQueryRepository.getAreaRequirementsWithCache(1L, ADMISSION_YEAR))
                .thenReturn(Collections.emptyList());
        when(graduationQueryRepository.getAreaRequirementsWithCache(2L, ADMISSION_YEAR))
                .thenReturn(sampleRequirements());

        List<AreaProgressDto> fallbackProgress = sampleProgress();
        when(graduationQueryRepository.getStudentAreaProgress(STUDENT_ID, 2L, ADMISSION_YEAR))
                .thenReturn(fallbackProgress);

        GraduationProgressResponse response = graduationService.getGraduationProgress(STUDENT_ID);

        assertThat(response.getGraduationProgress()).isEqualTo(fallbackProgress);
        InOrder inOrder = Mockito.inOrder(graduationQueryRepository);
        inOrder.verify(graduationQueryRepository).getAreaRequirementsWithCache(1L, ADMISSION_YEAR);
        inOrder.verify(graduationQueryRepository).getAreaRequirementsWithCache(2L, ADMISSION_YEAR);
        verify(graduationQueryRepository).getStudentAreaProgress(STUDENT_ID, 2L, ADMISSION_YEAR);
        verify(graduationQueryRepository, never()).getStudentAreaProgress(STUDENT_ID, 1L, ADMISSION_YEAR);
        verify(departmentRepository).findAllByEstablishedDepartmentName(DEPARTMENT_NAME);
    }

    @Test
    @DisplayName("모든 학과에서 졸업요건을 찾지 못하면 예외를 던진다")
    void getGraduationProgressThrowsWhenAllCandidatesFail() {
        Department department = mockDepartment(5L, DEPARTMENT_NAME);
        Student student = mockStudent(department, null);
        when(student.getStudentCode()).thenReturn("20221234");

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(academicCache.getGraduationProgress(STUDENT_ID)).thenReturn(null);
        Department candidate1 = mockDepartment(5L, DEPARTMENT_NAME);
        Department candidate2 = mockDepartment(6L, DEPARTMENT_NAME);
        when(departmentRepository.findAllByEstablishedDepartmentName(DEPARTMENT_NAME))
                .thenReturn(List.of(candidate1, candidate2));

        when(graduationQueryRepository.getAreaRequirementsWithCache(5L, ADMISSION_YEAR))
                .thenReturn(Collections.emptyList());
        when(graduationQueryRepository.getAreaRequirementsWithCache(6L, ADMISSION_YEAR))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> graduationService.getGraduationProgress(STUDENT_ID))
                .isInstanceOf(CommonException.class)
                .hasMessage(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.message());
        verify(departmentRepository).findAllByEstablishedDepartmentName(DEPARTMENT_NAME);
    }

    private Student mockStudent(Department department, Department major) {
        Student student = mock(Student.class);
        lenient().when(student.isTransferStudent()).thenReturn(false);
        lenient().when(student.getDepartment()).thenReturn(department);
        lenient().when(student.getMajor()).thenReturn(major);
        lenient().when(student.getSecondaryMajor()).thenReturn(null);
        AcademicInfo info = AcademicInfo.builder()
                .admissionYear(ADMISSION_YEAR)
                .isTransferStudent(false)
                .build();
        when(student.getAcademicInfo()).thenReturn(info);
        return student;
    }

    private Department mockDepartment(Long id, String name) {
        Department department = mock(Department.class);
        lenient().when(department.getId()).thenReturn(id);
        if (name != null) {
            lenient().when(department.getEstablishedDepartmentName()).thenReturn(name);
        }
        return department;
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

    private List<AreaRequirementDto> sampleRequirements() {
        return List.of(new AreaRequirementDto("전핵", 12, null, null));
    }
}
