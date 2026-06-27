// 학생별 졸업요건 진단 정보를 조회한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGraduationRequirementDiagnosticService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final GraduationQueryRepository graduationQueryRepository;

    public AdminTestDto.GraduationRequirementDiagnosticResponse diagnose(String studentCode) {
        String normalizedStudentCode = normalizeStudentCode(studentCode);
        Student student = studentRepository.findByStudentCode(normalizedStudentCode)
                .orElseThrow(() -> new CommonException(ErrorCode.STUDENT_NOT_FOUND));
        Integer admissionYear = student.getAcademicInfo() != null
                ? student.getAcademicInfo().getAdmissionYear()
                : null;
        Department primaryDepartment = student.getMajor() != null ? student.getMajor() : student.getDepartment();
        Department secondaryDepartment = student.getSecondaryMajor();
        List<AdminTestDto.GraduationRequirementCandidate> primaryCandidates =
                buildRequirementCandidates(resolveCandidateDepartments(primaryDepartment), admissionYear);
        List<AdminTestDto.GraduationRequirementCandidate> secondaryCandidates =
                buildRequirementCandidates(resolveCandidateDepartments(secondaryDepartment), admissionYear);
        List<AdminTestDto.DualMajorRequirementCandidate> dualMajorCandidates =
                buildDualMajorCandidates(primaryCandidates, secondaryCandidates, admissionYear);
        boolean dualMajor = secondaryDepartment != null;
        boolean progressResolvable = dualMajor
                ? hasResolvableDualMajor(primaryCandidates, dualMajorCandidates)
                : primaryCandidates.stream().anyMatch(candidate -> candidate.areaRequirementCount() > 0);

        return new AdminTestDto.GraduationRequirementDiagnosticResponse(
                normalizedStudentCode,
                admissionYear,
                student.isTransferStudent(),
                dualMajor ? "DUAL" : "SINGLE",
                progressResolvable,
                toDepartmentOption(student.getDepartment()),
                toDepartmentOption(primaryDepartment),
                toDepartmentOption(secondaryDepartment),
                primaryCandidates,
                secondaryCandidates,
                dualMajorCandidates
        );
    }

    private List<Department> resolveCandidateDepartments(Department baseDepartment) {
        List<Department> candidates = new ArrayList<>();
        addCandidate(candidates, baseDepartment);
        if (baseDepartment == null || baseDepartment.getEstablishedDepartmentName() == null) {
            return candidates;
        }
        String name = baseDepartment.getEstablishedDepartmentName().trim();
        if (name.isEmpty()) {
            return candidates;
        }
        List<Department> siblings = departmentRepository.findAllByEstablishedDepartmentName(name);
        if (siblings != null) {
            siblings.forEach(sibling -> addCandidate(candidates, sibling));
        }
        return candidates;
    }

    private List<AdminTestDto.GraduationRequirementCandidate> buildRequirementCandidates(
            List<Department> departments,
            Integer admissionYear
    ) {
        return departments.stream()
                .map(department -> new AdminTestDto.GraduationRequirementCandidate(
                        department.getId(),
                        department.getDepartmentCode(),
                        department.getEstablishedDepartmentName(),
                        countAreaRequirements(department.getId(), admissionYear)
                ))
                .toList();
    }

    private List<AdminTestDto.DualMajorRequirementCandidate> buildDualMajorCandidates(
            List<AdminTestDto.GraduationRequirementCandidate> primaryCandidates,
            List<AdminTestDto.GraduationRequirementCandidate> secondaryCandidates,
            Integer admissionYear
    ) {
        List<AdminTestDto.DualMajorRequirementCandidate> result = new ArrayList<>();
        for (AdminTestDto.GraduationRequirementCandidate primary : primaryCandidates) {
            for (AdminTestDto.GraduationRequirementCandidate secondary : secondaryCandidates) {
                int count = countDualMajorRequirements(primary.departmentId(), secondary.departmentId(), admissionYear);
                result.add(new AdminTestDto.DualMajorRequirementCandidate(primary.departmentId(), secondary.departmentId(), count));
            }
        }
        return result;
    }

    private boolean hasResolvableDualMajor(
            List<AdminTestDto.GraduationRequirementCandidate> primaryCandidates,
            List<AdminTestDto.DualMajorRequirementCandidate> dualMajorCandidates
    ) {
        return dualMajorCandidates.stream()
                .anyMatch(candidate -> candidate.dualRequirementCount() > 0
                        && hasPrimaryAreaRequirement(primaryCandidates, candidate.primaryDepartmentId()));
    }

    private boolean hasPrimaryAreaRequirement(
            List<AdminTestDto.GraduationRequirementCandidate> candidates,
            Long departmentId
    ) {
        return candidates.stream()
                .anyMatch(candidate -> candidate.departmentId().equals(departmentId)
                        && candidate.areaRequirementCount() > 0);
    }

    private int countAreaRequirements(Long departmentId, Integer admissionYear) {
        if (departmentId == null || admissionYear == null) {
            return 0;
        }
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getAreaRequirementsWithCache(departmentId, admissionYear);
        return requirements == null ? 0 : requirements.size();
    }

    private int countDualMajorRequirements(Long primaryId, Long secondaryId, Integer admissionYear) {
        if (primaryId == null || secondaryId == null || admissionYear == null) {
            return 0;
        }
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getDualMajorRequirementsWithCache(primaryId, secondaryId, admissionYear);
        return requirements == null ? 0 : requirements.size();
    }

    private void addCandidate(List<Department> candidates, Department department) {
        if (department != null && department.getId() != null
                && candidates.stream().noneMatch(candidate -> department.getId().equals(candidate.getId()))) {
            candidates.add(department);
        }
    }

    private AdminTestDto.DepartmentOption toDepartmentOption(Department department) {
        if (department == null) {
            return null;
        }
        return new AdminTestDto.DepartmentOption(
                department.getId(),
                department.getDepartmentCode(),
                department.getEstablishedDepartmentName()
        );
    }

    private String normalizeStudentCode(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        return studentCode.trim();
    }
}
