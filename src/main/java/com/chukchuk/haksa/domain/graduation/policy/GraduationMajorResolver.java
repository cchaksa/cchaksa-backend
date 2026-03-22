package com.chukchuk.haksa.domain.graduation.policy;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GraduationMajorResolver {

    private final GraduationQueryRepository graduationQueryRepository;
    private final DepartmentRepository departmentRepository;

    public MajorResolutionResult resolve(Student student, int admissionYear) {
        List<Long> primaryCandidates =
                resolveCandidateDepartmentIds(
                        student.getMajor() != null ? student.getMajor() : student.getDepartment()
                );

        List<Long> secondaryCandidates =
                student.getSecondaryMajor() == null
                        ? List.of()
                        : resolveCandidateDepartmentIds(student.getSecondaryMajor());

        if (secondaryCandidates.isEmpty()) {
            return resolveSingleMajor(primaryCandidates, admissionYear, student);
        }

        return resolveDualMajor(primaryCandidates, secondaryCandidates, admissionYear, student);
    }

    private MajorResolutionResult resolveSingleMajor(
            List<Long> primaryCandidates,
            int admissionYear,
            Student student
    ) {
        for (Long primaryId : primaryCandidates) {
            if (primaryId == null) continue;

            if (hasSingleMajorRequirement(primaryId, admissionYear)) {
                return new MajorResolutionResult(primaryId, null);
            }
        }

        throwNotFound(student,
                student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId(),
                null,
                admissionYear
        );
        return null;
    }

    private MajorResolutionResult resolveDualMajor(
            List<Long> primaryCandidates,
            List<Long> secondaryCandidates,
            int admissionYear,
            Student student
    ) {
        for (Long primaryId : primaryCandidates) {
            if (primaryId == null) continue;

            for (Long secondaryId : secondaryCandidates) {
                if (secondaryId == null) continue;

                if (hasDualMajorRequirement(primaryId, secondaryId, admissionYear)) {
                    return new MajorResolutionResult(primaryId, secondaryId);
                }
            }
        }

        throwNotFound(
                student,
                student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId(),
                student.getSecondaryMajor() != null ? student.getSecondaryMajor().getId() : null,
                admissionYear
        );
        return null;
    }

    private boolean hasSingleMajorRequirement(Long departmentId, int admissionYear) {
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getAreaRequirementsWithCache(departmentId, admissionYear);
        return requirements != null && !requirements.isEmpty();
    }

    private boolean hasDualMajorRequirement(Long primaryId, Long secondaryId, int admissionYear) {
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getDualMajorRequirementsWithCache(
                        primaryId, secondaryId, admissionYear
                );
        return requirements != null && !requirements.isEmpty();
    }

    private List<Long> resolveCandidateDepartmentIds(Department baseDepartment) {
        List<Long> candidateIds = new ArrayList<>();

        addCandidate(candidateIds, baseDepartment.getId());

        String establishedName = baseDepartment.getEstablishedDepartmentName();
        if (establishedName != null && !establishedName.trim().isEmpty()) {
            List<Department> siblings =
                    departmentRepository.findAllByEstablishedDepartmentName(establishedName.trim());
            if (siblings != null) {
                for (Department sibling : siblings) {
                    addCandidate(candidateIds, sibling.getId());
                }
            }
        }
        return candidateIds;
    }

    private void addCandidate(List<Long> candidateIds, Long departmentId) {
        if (departmentId != null && !candidateIds.contains(departmentId)) {
            candidateIds.add(departmentId);
        }
    }

    private void throwNotFound(
            Student student,
            Long primaryMajorId,
            Long secondaryMajorId,
            int admissionYear
    ) {
        MDC.put("student_code", student.getStudentCode());
        MDC.put("admission_year", String.valueOf(admissionYear));
        MDC.put("primary_department_id", String.valueOf(primaryMajorId));
        MDC.put("secondary_department_id",
                secondaryMajorId == null ? "NONE" : String.valueOf(secondaryMajorId));
        MDC.put("major_type", secondaryMajorId == null ? "SINGLE" : "DUAL");

        throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND);
    }
}