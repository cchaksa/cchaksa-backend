// 사용자 학과별 외국어 인증 기준 조회 정책을 검증하는 테스트
package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.domain.graduation.model.DepartmentLanguageCertPolicyMapping;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertMatchStatus;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertRequirement;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertTestType;
import com.chukchuk.haksa.domain.graduation.repository.DepartmentLanguageCertPolicyMappingRepository;
import com.chukchuk.haksa.domain.graduation.repository.LanguageCertRequirementRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageCertRequirementServiceTests {

    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Mock
    private StudentService studentService;

    @Mock
    private DepartmentLanguageCertPolicyMappingRepository mappingRepository;

    @Mock
    private LanguageCertRequirementRepository requirementRepository;

    @InjectMocks
    private LanguageCertRequirementService service;

    @Test
    @DisplayName("전공 코드와 입학년도로 확정 매핑의 외국어 인증 기준을 반환한다")
    void getRequirementUsesMajorDepartmentCode() {
        Department department = new Department("2000513", "정보통신학부");
        Department major = new Department("2000514", "컴퓨터SW");
        Student student = mockStudent(department, major, 2021);
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_OTHER",
                "ICT융합대학 그외학부",
                null
        );
        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.verified(
                        "2000514",
                        2021,
                        9999,
                        group,
                        "컴퓨터SW 21학번 이후 기준"
                );
        LanguageCertRequirement toeic = LanguageCertRequirement.score(
                group,
                LanguageCertTestType.TOEIC,
                600,
                "TOEIC 600점 이상",
                1
        );

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(mappingRepository.findApplicableMappings("2000514", 2021))
                .thenReturn(List.of(mapping));
        when(requirementRepository.findAllByPolicyGroupOrderBySortOrderAsc(group))
                .thenReturn(List.of(toeic));

        LanguageCertRequirementResponse response = service.getRequirement(STUDENT_ID);

        assertThat(response.departmentCode()).isEqualTo("2000514");
        assertThat(response.departmentName()).isEqualTo("컴퓨터SW");
        assertThat(response.admissionYear()).isEqualTo(2021);
        assertThat(response.policyGroupKey()).isEqualTo("ICT_OTHER");
        assertThat(response.policyGroupName()).isEqualTo("ICT융합대학 그외학부");
        assertThat(response.matchStatus()).isEqualTo(LanguageCertMatchStatus.VERIFIED);
        assertThat(response.note()).isEqualTo("컴퓨터SW 21학번 이후 기준");
        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).testType()).isEqualTo(LanguageCertTestType.TOEIC);
        assertThat(response.requirements().get(0).minimumScore()).isEqualTo(600);
    }

    @Test
    @DisplayName("전공이 없으면 소속 학과 코드로 외국어 인증 기준을 조회한다")
    void getRequirementFallsBackToDepartmentWhenMajorMissing() {
        Department department = new Department("2000511", "데이터과학부");
        Student student = mockStudent(department, null, 2021);
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_DATA_SCIENCE",
                "ICT융합대학 데이터과학부",
                null
        );
        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.verified(
                        "2000511",
                        2021,
                        9999,
                        group,
                        "데이터과학부 기준"
                );

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(mappingRepository.findApplicableMappings("2000511", 2021))
                .thenReturn(List.of(mapping));
        when(requirementRepository.findAllByPolicyGroupOrderBySortOrderAsc(group))
                .thenReturn(List.of());

        LanguageCertRequirementResponse response = service.getRequirement(STUDENT_ID);

        assertThat(response.departmentCode()).isEqualTo("2000511");
        assertThat(response.departmentName()).isEqualTo("데이터과학부");
        verify(mappingRepository).findApplicableMappings("2000511", 2021);
    }

    @Test
    @DisplayName("추정 매핑도 기준을 반환하고 INFERRED 상태를 유지한다")
    void getRequirementReturnsInferredMappingWithRequirements() {
        Department major = new Department("2000757", "AI데이터과학부");
        Student student = mockStudent(new Department("2000754", "경영공학대학"), major, 2025);
        LanguageCertPolicyGroup group = LanguageCertPolicyGroup.create(
                "ICT_DATA_SCIENCE",
                "ICT융합대학 데이터과학부",
                null
        );
        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.inferred(
                        "2000757",
                        2025,
                        9999,
                        group,
                        "AI데이터과학부는 데이터과학부 기준 공유 추정"
                );

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(mappingRepository.findApplicableMappings("2000757", 2025))
                .thenReturn(List.of(mapping));
        when(requirementRepository.findAllByPolicyGroupOrderBySortOrderAsc(group))
                .thenReturn(List.of(LanguageCertRequirement.grade(
                        group,
                        LanguageCertTestType.OPIC,
                        "IM1",
                        "OPIc IM1 이상",
                        4
                )));

        LanguageCertRequirementResponse response = service.getRequirement(STUDENT_ID);

        assertThat(response.matchStatus()).isEqualTo(LanguageCertMatchStatus.INFERRED);
        assertThat(response.requirements()).hasSize(1);
        assertThat(response.requirements().get(0).minimumGrade()).isEqualTo("IM1");
    }

    @Test
    @DisplayName("미매핑 상태는 200 응답용 빈 기준 목록으로 반환한다")
    void getRequirementReturnsUnmappedWhenMappingIsUnmapped() {
        Department department = new Department("2000763", "자유전공학부");
        Student student = mockStudent(department, null, 2025);
        DepartmentLanguageCertPolicyMapping mapping =
                DepartmentLanguageCertPolicyMapping.unmapped(
                        "2000763",
                        2021,
                        9999,
                        "기준표에 직접 행이 없음"
                );

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(mappingRepository.findApplicableMappings("2000763", 2025))
                .thenReturn(List.of(mapping));

        LanguageCertRequirementResponse response = service.getRequirement(STUDENT_ID);

        assertThat(response.matchStatus()).isEqualTo(LanguageCertMatchStatus.UNMAPPED);
        assertThat(response.policyGroupKey()).isNull();
        assertThat(response.requirements()).isEmpty();
        assertThat(response.note()).isEqualTo("기준표에 직접 행이 없음");
    }

    @Test
    @DisplayName("매핑 row가 없으면 미매핑 응답을 반환한다")
    void getRequirementReturnsUnmappedWhenMappingMissing() {
        Department department = new Department("9999999", "신설학과");
        Student student = mockStudent(department, null, 2026);

        when(studentService.getStudentById(STUDENT_ID)).thenReturn(student);
        when(mappingRepository.findApplicableMappings("9999999", 2026))
                .thenReturn(List.of());

        LanguageCertRequirementResponse response = service.getRequirement(STUDENT_ID);

        assertThat(response.departmentCode()).isEqualTo("9999999");
        assertThat(response.matchStatus()).isEqualTo(LanguageCertMatchStatus.UNMAPPED);
        assertThat(response.requirements()).isEmpty();
    }

    private Student mockStudent(Department department, Department major, int admissionYear) {
        Student student = mock(Student.class);
        AcademicInfo academicInfo = AcademicInfo.builder()
                .admissionYear(admissionYear)
                .build();
        lenient().when(student.getDepartment()).thenReturn(department);
        lenient().when(student.getMajor()).thenReturn(major);
        lenient().when(student.getAcademicInfo()).thenReturn(academicInfo);
        return student;
    }
}
