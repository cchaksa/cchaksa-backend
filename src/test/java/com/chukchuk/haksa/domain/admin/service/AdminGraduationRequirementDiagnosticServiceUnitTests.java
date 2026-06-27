// dev 졸업요건 진단 서비스 동작을 검증한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGraduationRequirementDiagnosticServiceUnitTests {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private GraduationQueryRepository graduationQueryRepository;

    @InjectMocks
    private AdminGraduationRequirementDiagnosticService service;

    @Test
    @DisplayName("단일전공 학생은 후보 학과별 졸업요건 개수와 진행 가능 여부를 반환한다")
    void diagnose_singleMajor_returnsRequirementCandidates() {
        Department department = department(1L, "CSE", "컴퓨터학과");
        Department sibling = department(2L, "CSE_OLD", "컴퓨터학과");
        Student student = student("20240001", department, null, null);
        when(studentRepository.findByStudentCode("20240001")).thenReturn(Optional.of(student));
        when(departmentRepository.findAllByEstablishedDepartmentName("컴퓨터학과"))
                .thenReturn(List.of(department, sibling));
        when(graduationQueryRepository.getAreaRequirementsWithCache(1L, 2024))
                .thenReturn(List.of(requirement("전핵")));
        when(graduationQueryRepository.getAreaRequirementsWithCache(2L, 2024)).thenReturn(List.of());

        var response = service.diagnose("20240001");

        assertThat(response.majorType()).isEqualTo("SINGLE");
        assertThat(response.progressResolvable()).isTrue();
        assertThat(response.primaryCandidates()).extracting("departmentId").containsExactly(1L, 2L);
        assertThat(response.primaryCandidates()).extracting("areaRequirementCount").containsExactly(1, 0);
    }

    @Test
    @DisplayName("복수전공 학생은 조합별 복수전공 요건 개수와 진행 가능 여부를 반환한다")
    void diagnose_dualMajor_returnsDualRequirementCandidates() {
        Department department = department(1L, "BASE", "소프트웨어융합대학");
        Department major = department(10L, "CSE", "컴퓨터학과");
        Department secondary = department(20L, "BUS", "경영학과");
        Student student = student("20240001", department, major, secondary);
        when(studentRepository.findByStudentCode("20240001")).thenReturn(Optional.of(student));
        when(departmentRepository.findAllByEstablishedDepartmentName("컴퓨터학과")).thenReturn(List.of(major));
        when(departmentRepository.findAllByEstablishedDepartmentName("경영학과")).thenReturn(List.of(secondary));
        when(graduationQueryRepository.getAreaRequirementsWithCache(10L, 2024))
                .thenReturn(List.of(requirement("전핵")));
        when(graduationQueryRepository.getAreaRequirementsWithCache(20L, 2024)).thenReturn(List.of());
        when(graduationQueryRepository.getDualMajorRequirementsWithCache(10L, 20L, 2024))
                .thenReturn(List.of(requirement("복핵"), requirement("복선")));

        var response = service.diagnose("20240001");

        assertThat(response.majorType()).isEqualTo("DUAL");
        assertThat(response.progressResolvable()).isTrue();
        assertThat(response.dualMajorCandidates()).hasSize(1);
        assertThat(response.dualMajorCandidates().get(0).dualRequirementCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("학번에 해당하는 학생이 없으면 학생 없음 예외를 던진다")
    void diagnose_unknownStudent_throwsStudentNotFound() {
        when(studentRepository.findByStudentCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.diagnose("unknown"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode())
                        .isEqualTo(ErrorCode.STUDENT_NOT_FOUND.code()));
    }

    private Student student(String studentCode, Department department, Department major, Department secondaryMajor) {
        return Student.builder()
                .studentCode(studentCode)
                .name("테스트")
                .department(department)
                .major(major)
                .secondaryMajor(secondaryMajor)
                .admissionYear(2024)
                .isTransferStudent(false)
                .build();
    }

    private Department department(Long id, String code, String name) {
        Department department = new Department(code, name);
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private AreaRequirementDto requirement(String areaType) {
        return new AreaRequirementDto(areaType, 3, null, null);
    }
}
