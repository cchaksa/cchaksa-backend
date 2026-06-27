// PDF 템플릿을 기준으로 누락 졸업요건을 생성한다
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminGraduationRequirementCreationService {

    private static final String AREA_REQUIREMENT_TABLE = "department_area_requirements";
    private static final String DUAL_REQUIREMENT_TABLE = "dual_major_requirements";

    private final StudentRepository studentRepository;
    private final DepartmentAreaRequirementRepository areaRequirementRepository;
    private final DualMajorRequirementRepository dualMajorRequirementRepository;
    private final AdminGraduationRequirementTemplateService templateService;
    private final AcademicCache academicCache;

    public AdminTestDto.CreateMissingGraduationRequirementsResponse createMissing(
            AdminTestDto.CreateMissingGraduationRequirementsRequest request
    ) {
        String studentCode = normalizeStudentCode(request.studentCode());
        boolean dryRun = request.dryRun() == null || request.dryRun();
        Student student = studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new CommonException(ErrorCode.STUDENT_NOT_FOUND));
        Integer admissionYear = student.getAcademicInfo() != null
                ? student.getAcademicInfo().getAdmissionYear()
                : null;
        if (admissionYear == null) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        Department primary = student.getMajor() != null ? student.getMajor() : student.getDepartment();
        if (primary == null) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        Department secondary = student.getSecondaryMajor();
        AdminGraduationRequirementTemplate primaryTemplate = getTemplate(admissionYear, primary);
        AdminGraduationRequirementTemplate secondaryTemplate = secondary != null
                ? getTemplate(admissionYear, secondary)
                : null;
        List<AdminTestDto.GraduationRequirementCreationTarget> singleTargets =
                buildSingleTargets(primary, admissionYear, primaryTemplate);
        List<AdminTestDto.GraduationRequirementCreationTarget> dualTargets =
                buildDualTargets(primary, secondary, admissionYear, primaryTemplate, secondaryTemplate);
        int missingSingle = countMissing(singleTargets);
        int missingDual = countMissing(dualTargets);
        if (!dryRun) {
            saveSingleTargets(primary, admissionYear, singleTargets);
            saveDualTargets(primary, secondary, admissionYear, dualTargets);
            refreshCache(student, primary, secondary, admissionYear, primaryTemplate, secondaryTemplate);
        }
        List<AdminTestDto.GraduationRequirementCreationTarget> responseSingleTargets =
                dryRun ? singleTargets : markCreated(singleTargets);
        List<AdminTestDto.GraduationRequirementCreationTarget> responseDualTargets =
                dryRun ? dualTargets : markCreated(dualTargets);
        return new AdminTestDto.CreateMissingGraduationRequirementsResponse(
                studentCode,
                admissionYear,
                dryRun,
                !dryRun,
                toTemplateMatch(primaryTemplate),
                toTemplateMatch(secondaryTemplate),
                missingSingle,
                dryRun ? 0 : missingSingle,
                missingDual,
                dryRun ? 0 : missingDual,
                responseSingleTargets,
                responseDualTargets
        );
    }

    private List<AdminTestDto.GraduationRequirementCreationTarget> buildSingleTargets(
            Department department,
            Integer admissionYear,
            AdminGraduationRequirementTemplate template
    ) {
        return template.singleMajorRequirements().stream()
                .map(requirement -> new AdminTestDto.GraduationRequirementCreationTarget(
                        AREA_REQUIREMENT_TABLE,
                        department.getId(),
                        department.getEstablishedDepartmentName(),
                        null,
                        requirement.areaType(),
                        requirement.requiredCredits(),
                        areaRequirementRepository.existsByDepartmentIdAndAdmissionYearAndAreaType(
                                department.getId(),
                                admissionYear,
                                requirement.areaType()
                        ),
                        false
                ))
                .toList();
    }

    private List<AdminTestDto.GraduationRequirementCreationTarget> buildDualTargets(
            Department primary,
            Department secondary,
            Integer admissionYear,
            AdminGraduationRequirementTemplate primaryTemplate,
            AdminGraduationRequirementTemplate secondaryTemplate
    ) {
        if (secondary == null) {
            return List.of();
        }
        List<AdminTestDto.GraduationRequirementCreationTarget> primaryTargets =
                buildDualTargetsForRole(primary, admissionYear, primaryTemplate, MajorRole.PRIMARY);
        List<AdminTestDto.GraduationRequirementCreationTarget> secondaryTargets =
                buildDualTargetsForRole(secondary, admissionYear, secondaryTemplate, MajorRole.SECONDARY);
        return java.util.stream.Stream.concat(primaryTargets.stream(), secondaryTargets.stream()).toList();
    }

    private List<AdminTestDto.GraduationRequirementCreationTarget> buildDualTargetsForRole(
            Department department,
            Integer admissionYear,
            AdminGraduationRequirementTemplate template,
            MajorRole role
    ) {
        return template.dualMajorRequirements().stream()
                .filter(requirement -> requirement.majorRole() == role)
                .map(requirement -> new AdminTestDto.GraduationRequirementCreationTarget(
                        DUAL_REQUIREMENT_TABLE,
                        department.getId(),
                        department.getEstablishedDepartmentName(),
                        role.name(),
                        requirement.areaType(),
                        requirement.requiredCredits(),
                        dualMajorRequirementRepository.existsByDepartmentIdAndAdmissionYearAndMajorRoleAndAreaType(
                                department.getId(),
                                admissionYear,
                                role,
                                requirement.areaType()
                        ),
                        false
                ))
                .toList();
    }

    private List<AdminTestDto.GraduationRequirementCreationTarget> markCreated(
            List<AdminTestDto.GraduationRequirementCreationTarget> targets
    ) {
        return targets.stream()
                .map(target -> new AdminTestDto.GraduationRequirementCreationTarget(
                        target.targetTable(),
                        target.departmentId(),
                        target.departmentName(),
                        target.majorRole(),
                        target.areaType(),
                        target.requiredCredits(),
                        target.alreadyExists(),
                        !target.alreadyExists()
                ))
                .toList();
    }

    private void saveSingleTargets(
            Department department,
            Integer admissionYear,
            List<AdminTestDto.GraduationRequirementCreationTarget> targets
    ) {
        List<DepartmentAreaRequirement> requirements = targets.stream()
                .filter(target -> !target.alreadyExists())
                .map(target -> DepartmentAreaRequirement.create(
                        department,
                        admissionYear,
                        target.areaType(),
                        target.requiredCredits()
                ))
                .toList();
        if (!requirements.isEmpty()) {
            areaRequirementRepository.saveAll(requirements);
        }
    }

    private void saveDualTargets(
            Department primary,
            Department secondary,
            Integer admissionYear,
            List<AdminTestDto.GraduationRequirementCreationTarget> targets
    ) {
        List<DualMajorRequirement> requirements = targets.stream()
                .filter(target -> !target.alreadyExists())
                .map(target -> DualMajorRequirement.create(
                        "PRIMARY".equals(target.majorRole()) ? primary : secondary,
                        admissionYear,
                        MajorRole.valueOf(target.majorRole()),
                        target.areaType(),
                        target.requiredCredits()
                ))
                .toList();
        if (!requirements.isEmpty()) {
            dualMajorRequirementRepository.saveAll(requirements);
        }
    }

    private void refreshCache(
            Student student,
            Department primary,
            Department secondary,
            Integer admissionYear,
            AdminGraduationRequirementTemplate primaryTemplate,
            AdminGraduationRequirementTemplate secondaryTemplate
    ) {
        academicCache.deleteAllByStudentId(student.getId());
        academicCache.setGraduationRequirements(
                primary.getId(),
                admissionYear,
                toAreaDtos(primaryTemplate.singleMajorRequirements())
        );
        if (secondary != null) {
            List<AreaRequirementDto> dualRequirements = java.util.stream.Stream.concat(
                    toAreaDtos(primaryTemplate.dualMajorRequirements(), MajorRole.PRIMARY).stream(),
                    toAreaDtos(secondaryTemplate.dualMajorRequirements(), MajorRole.SECONDARY).stream()
            ).toList();
            academicCache.setDualMajorRequirements(primary.getId(), secondary.getId(), admissionYear, dualRequirements);
        }
    }

    private AdminGraduationRequirementTemplate getTemplate(Integer admissionYear, Department department) {
        return templateService.findByAdmissionYearAndDepartmentName(
                        admissionYear,
                        department.getEstablishedDepartmentName()
                )
                .orElseThrow(() -> new CommonException(ErrorCode.INVALID_ARGUMENT));
    }

    private AdminTestDto.GraduationRequirementTemplateMatch toTemplateMatch(
            AdminGraduationRequirementTemplate template
    ) {
        if (template == null) {
            return null;
        }
        return new AdminTestDto.GraduationRequirementTemplateMatch(
                template.sourcePdf(),
                template.sourcePage(),
                template.departmentName(),
                template.majorName()
        );
    }

    private List<AreaRequirementDto> toAreaDtos(
            List<AdminGraduationRequirementTemplate.AreaRequirement> requirements
    ) {
        return requirements.stream()
                .map(requirement -> new AreaRequirementDto(requirement.areaType(), requirement.requiredCredits(), null, null))
                .toList();
    }

    private List<AreaRequirementDto> toAreaDtos(
            List<AdminGraduationRequirementTemplate.DualMajorRequirement> requirements,
            MajorRole role
    ) {
        return requirements.stream()
                .filter(requirement -> requirement.majorRole() == role)
                .map(requirement -> new AreaRequirementDto(requirement.areaType(), requirement.requiredCredits(), null, null))
                .toList();
    }

    private int countMissing(List<AdminTestDto.GraduationRequirementCreationTarget> targets) {
        return (int) targets.stream()
                .filter(target -> !target.alreadyExists())
                .count();
    }

    private String normalizeStudentCode(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        return studentCode.trim();
    }
}
