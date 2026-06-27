// PDF 템플릿 기반 졸업요건 생성 서비스를 검증한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminGraduationRequirementTemplate;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.model.DepartmentAreaRequirement;
import com.chukchuk.haksa.domain.department.model.DualMajorRequirement;
import com.chukchuk.haksa.domain.department.model.MajorRole;
import com.chukchuk.haksa.domain.department.repository.DepartmentAreaRequirementRepository;
import com.chukchuk.haksa.domain.department.repository.DualMajorRequirementRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGraduationRequirementCreationServiceUnitTests {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentAreaRequirementRepository areaRequirementRepository;

    @Mock
    private DualMajorRequirementRepository dualMajorRequirementRepository;

    @Mock
    private AdminGraduationRequirementTemplateService templateService;

    @Mock
    private AcademicCache academicCache;

    @InjectMocks
    private AdminGraduationRequirementCreationService service;

    @Test
    @DisplayName("학생의 주전공과 소속 학과가 모두 없으면 잘못된 요청으로 실패한다")
    void createMissing_withoutPrimaryDepartment_failsWithInvalidArgument() {
        Student student = student("20260001", null, null, null);
        when(studentRepository.findByStudentCode("20260001")).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.createMissing(
                new AdminTestDto.CreateMissingGraduationRequirementsRequest("20260001", true)
        ))
                .isInstanceOf(CommonException.class)
                .extracting(ex -> ((CommonException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        verifyNoInteractions(templateService);
    }

    @Test
    @DisplayName("dry-run이면 누락 졸업요건을 저장하지 않고 생성 대상을 반환한다")
    void createMissing_dryRun_returnsTargetsWithoutSaving() {
        Department department = department(1L, "CSE", "컴퓨터공학");
        Student student = student("20260001", department, null, null);
        when(studentRepository.findByStudentCode("20260001")).thenReturn(Optional.of(student));
        when(templateService.findByAdmissionYearAndDepartmentName(2026, "컴퓨터공학"))
                .thenReturn(Optional.of(template("컴퓨터공학")));
        when(areaRequirementRepository.existsByDepartmentIdAndAdmissionYearAndAreaType(1L, 2026, "전핵"))
                .thenReturn(true);
        when(areaRequirementRepository.existsByDepartmentIdAndAdmissionYearAndAreaType(1L, 2026, "전선"))
                .thenReturn(false);

        AdminTestDto.CreateMissingGraduationRequirementsResponse response = service.createMissing(
                new AdminTestDto.CreateMissingGraduationRequirementsRequest("20260001", true)
        );

        assertThat(response.dryRun()).isTrue();
        assertThat(response.missingSingleMajorCount()).isEqualTo(1);
        assertThat(response.createdSingleMajorCount()).isZero();
        assertThat(response.singleMajorRequirements()).extracting("areaType").containsExactly("전핵", "전선");
        verify(areaRequirementRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("apply이면 누락된 단일전공과 복수전공 졸업요건을 저장한다")
    void createMissing_apply_savesMissingRequirements() {
        Department primary = department(1L, "CSE", "컴퓨터공학");
        Department secondary = department(2L, "BUS", "경영학");
        Student student = student("20260001", primary, primary, secondary);
        when(studentRepository.findByStudentCode("20260001")).thenReturn(Optional.of(student));
        when(templateService.findByAdmissionYearAndDepartmentName(2026, "컴퓨터공학"))
                .thenReturn(Optional.of(template("컴퓨터공학")));
        when(templateService.findByAdmissionYearAndDepartmentName(2026, "경영학"))
                .thenReturn(Optional.of(template("경영학")));
        when(areaRequirementRepository.findAllByDepartmentIdAndAdmissionYear(1L, 2026))
                .thenReturn(List.of(DepartmentAreaRequirement.create(primary, 2026, "DB전핵", 99)));
        when(dualMajorRequirementRepository.findAllByDepartmentIdAndAdmissionYearAndMajorRole(1L, 2026, MajorRole.PRIMARY))
                .thenReturn(List.of(DualMajorRequirement.create(primary, 2026, MajorRole.PRIMARY, "DB복선", 6)));
        when(dualMajorRequirementRepository.findAllByDepartmentIdAndAdmissionYearAndMajorRole(2L, 2026, MajorRole.SECONDARY))
                .thenReturn(List.of(DualMajorRequirement.create(secondary, 2026, MajorRole.SECONDARY, "DB복핵", 27)));

        AdminTestDto.CreateMissingGraduationRequirementsResponse response = service.createMissing(
                new AdminTestDto.CreateMissingGraduationRequirementsRequest("20260001", false)
        );

        ArgumentCaptor<List<DepartmentAreaRequirement>> areaCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<DualMajorRequirement>> dualCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AreaRequirementDto>> areaCacheCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AreaRequirementDto>> dualCacheCaptor = ArgumentCaptor.forClass(List.class);
        verify(areaRequirementRepository).saveAll(areaCaptor.capture());
        verify(dualMajorRequirementRepository).saveAll(dualCaptor.capture());
        verify(academicCache).deleteAllByStudentId(student.getId());
        verify(academicCache).setGraduationRequirements(eq(1L), eq(2026), areaCacheCaptor.capture());
        verify(academicCache).setDualMajorRequirements(eq(1L), eq(2L), eq(2026), dualCacheCaptor.capture());
        assertThat(areaCaptor.getValue()).hasSize(2);
        assertThat(dualCaptor.getValue()).hasSize(2);
        assertThat(areaCacheCaptor.getValue()).containsExactly(new AreaRequirementDto("DB전핵", 99, null, null));
        assertThat(dualCacheCaptor.getValue()).containsExactly(
                new AreaRequirementDto("DB복선", 6, null, null),
                new AreaRequirementDto("DB복핵", 27, null, null)
        );
        assertThat(response.createdSingleMajorCount()).isEqualTo(2);
        assertThat(response.createdDualMajorCount()).isEqualTo(2);
        assertThat(response.singleMajorRequirements())
                .extracting(AdminTestDto.GraduationRequirementCreationTarget::created)
                .containsOnly(true);
        assertThat(response.dualMajorRequirements())
                .extracting(AdminTestDto.GraduationRequirementCreationTarget::created)
                .containsOnly(true);
    }

    private Student student(String studentCode, Department department, Department major, Department secondaryMajor) {
        Student student = Student.builder()
                .studentCode(studentCode)
                .name("테스트")
                .department(department)
                .major(major)
                .secondaryMajor(secondaryMajor)
                .admissionYear(2026)
                .isTransferStudent(false)
                .build();
        ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
        return student;
    }

    private Department department(Long id, String code, String name) {
        Department department = new Department(code, name);
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private AdminGraduationRequirementTemplate template(String name) {
        return new AdminGraduationRequirementTemplate(
                2026,
                "2026_credits.pdf",
                1,
                "테스트대학",
                name,
                name,
                List.of(name),
                130,
                List.of(
                        new AdminGraduationRequirementTemplate.AreaRequirement("전핵", 24),
                        new AdminGraduationRequirementTemplate.AreaRequirement("전선", 51)
                ),
                List.of(
                        new AdminGraduationRequirementTemplate.DualMajorRequirement(MajorRole.PRIMARY, "복선", 6),
                        new AdminGraduationRequirementTemplate.DualMajorRequirement(MajorRole.SECONDARY, "복핵", 27)
                )
        );
    }
}
