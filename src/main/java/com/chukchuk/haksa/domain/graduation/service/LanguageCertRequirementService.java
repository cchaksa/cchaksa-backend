// 로그인 학생에게 적용되는 외국어 인증 기준을 조회하는 서비스
package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.domain.graduation.model.DepartmentLanguageCertPolicyMapping;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertMatchStatus;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import com.chukchuk.haksa.domain.graduation.repository.DepartmentLanguageCertPolicyMappingRepository;
import com.chukchuk.haksa.domain.graduation.repository.LanguageCertRequirementRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguageCertRequirementService {

    private static final String MAPPING_REQUIRED_NOTE = "외국어 인증 기준 매핑이 필요합니다.";

    private final StudentService studentService;
    private final DepartmentLanguageCertPolicyMappingRepository mappingRepository;
    private final LanguageCertRequirementRepository requirementRepository;

    public LanguageCertRequirementResponse getRequirement(UUID studentId) {
        Student student = studentService.getStudentById(studentId);
        Department baseDepartment = resolveBaseDepartment(student);
        Integer admissionYear = student.getAcademicInfo().getAdmissionYear();
        String departmentCode = baseDepartment.getDepartmentCode();
        String departmentName = baseDepartment.getEstablishedDepartmentName();

        List<DepartmentLanguageCertPolicyMapping> mappings =
                mappingRepository.findApplicableMappings(departmentCode, admissionYear);

        if (mappings.isEmpty()) {
            return LanguageCertRequirementResponse.unmapped(
                    departmentCode,
                    departmentName,
                    admissionYear,
                    MAPPING_REQUIRED_NOTE
            );
        }

        DepartmentLanguageCertPolicyMapping mapping = mappings.get(0);
        if (mapping.getMatchStatus() == LanguageCertMatchStatus.UNMAPPED
                || mapping.getPolicyGroup() == null) {
            return LanguageCertRequirementResponse.unmapped(
                    departmentCode,
                    departmentName,
                    admissionYear,
                    mapping.getNote()
            );
        }

        LanguageCertPolicyGroup policyGroup = mapping.getPolicyGroup();
        List<LanguageCertRequirementResponse.Requirement> requirements =
                requirementRepository.findAllByPolicyGroupOrderBySortOrderAsc(policyGroup)
                        .stream()
                        .map(LanguageCertRequirementResponse.Requirement::from)
                        .toList();

        return new LanguageCertRequirementResponse(
                departmentCode,
                departmentName,
                admissionYear,
                policyGroup.getGroupKey(),
                policyGroup.getName(),
                mapping.getMatchStatus(),
                mapping.getNote(),
                requirements
        );
    }

    private Department resolveBaseDepartment(Student student) {
        return student.getMajor() != null ? student.getMajor() : student.getDepartment();
    }
}
