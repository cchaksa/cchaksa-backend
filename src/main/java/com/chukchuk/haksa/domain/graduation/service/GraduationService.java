package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GraduationService {

    private static final int SPECIAL_YEAR = 2025;
    private static final Set<Long> SPECIAL_DEPTS = Set.of(30L, 115L, 127L);

    private final StudentService studentService;
    private final GraduationQueryRepository graduationQueryRepository;
    private final AcademicCache academicCache;
    private final DepartmentRepository departmentRepository;

    /* 졸업 요건 진행 상황 조회 */
    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        try {
            GraduationProgressResponse cached = academicCache.getGraduationProgress(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] graduation.progress.cache.get.fail studentId={} ex={}",
                    studentId, e.getClass().getSimpleName(), e);
        }

        Student student = studentService.getStudentById(studentId);
        validateTransferStudent(student);

        int admissionYear = student.getAcademicInfo().getAdmissionYear();

        MajorResolutionResult majorResolution =
                resolveMajorForGraduationRequirement(student, admissionYear);

        List<AreaProgressDto> areaProgress =
                resolveAreaProgress(
                        student,
                        studentId,
                        majorResolution.primaryMajorId(),
                        majorResolution.secondaryMajorId(),
                        admissionYear
                );

        GraduationProgressResponse response = new GraduationProgressResponse(areaProgress);

        if (isDifferentGradRequirement(majorResolution.primaryMajorId(), admissionYear)) {
            response.setHasDifferentGraduationRequirement();
        }

        try {
            academicCache.setGraduationProgress(studentId, response);
        } catch (Exception e) {
            log.warn("[BIZ] graduation.progress.cache.set.fail studentId={} ex={}",
                    studentId, e.getClass().getSimpleName(), e);
        }

        return response;
    }

    // ==============================
    // Major Resolution
    // ==============================
    private MajorResolutionResult resolveMajorForGraduationRequirement(
            Student student,
            int admissionYear
    ) {
        List<Long> primaryCandidates = resolveCandidateDepartmentIds(student);
        List<Long> secondaryCandidates = resolveSecondaryCandidateIds(student);

        boolean isSingleMajor = secondaryCandidates.isEmpty();

        for (Long primaryId : primaryCandidates) {
            if (primaryId == null) continue;

            if (isSingleMajor) {
                if (hasSingleMajorRequirement(primaryId, admissionYear)) {
                    return new MajorResolutionResult(primaryId, null);
                }
                continue;
            }

            for (Long secondaryId : secondaryCandidates) {
                if (secondaryId == null) continue;

                if (hasDualMajorRequirement(primaryId, secondaryId, admissionYear)) {
                    return new MajorResolutionResult(primaryId, secondaryId);
                }
            }
        }

        throwGraduationRequirementNotFound(
                student,
                student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId(),
                student.getSecondaryMajor() != null ? student.getSecondaryMajor().getId() : null,
                admissionYear
        );

        return null; // unreachable
    }

    private boolean hasSingleMajorRequirement(Long departmentId, int admissionYear) {
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getAreaRequirementsWithCache(
                        departmentId,
                        admissionYear
                );
        return requirements != null && !requirements.isEmpty();
    }

    private boolean hasDualMajorRequirement(
            Long primaryId,
            Long secondaryId,
            int admissionYear
    ) {
        List<AreaRequirementDto> requirements =
                graduationQueryRepository.getDualMajorRequirementsWithCache(
                        primaryId,
                        secondaryId,
                        admissionYear
                );
        return requirements != null && !requirements.isEmpty();
    }

    // ==============================
    // Progress Resolution
    // ==============================

    private List<AreaProgressDto> resolveAreaProgress(
            Student student,
            UUID studentId,
            Long primaryMajorId,
            Long secondaryMajorId,
            int admissionYear
    ) {
        if (secondaryMajorId == null) {
            return getSingleMajorProgressOrThrow(
                    student, studentId, primaryMajorId, admissionYear
            );
        }

        return getDualMajorProgressOrThrow(
                student, studentId, primaryMajorId, secondaryMajorId, admissionYear
        );
    }

    private List<AreaProgressDto> getSingleMajorProgressOrThrow(
            Student student,
            UUID studentId,
            Long departmentId,
            int admissionYear
    ) {
        List<AreaProgressDto> result =
                graduationQueryRepository.getStudentAreaProgress(
                        studentId, departmentId, admissionYear
                );

        if (result.isEmpty()) {
            throwGraduationRequirementNotFound(
                    student,
                    departmentId,
                    null,
                    admissionYear
            );
        }
        return result;
    }

    private List<AreaProgressDto> getDualMajorProgressOrThrow(
            Student student,
            UUID studentId,
            Long primaryMajorId,
            Long secondaryMajorId,
            int admissionYear
    ) {
        try {
            return graduationQueryRepository.getDualMajorAreaProgress(
                    studentId,
                    primaryMajorId,
                    secondaryMajorId,
                    admissionYear
            );
        } catch (CommonException e) {
            if (ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND.code().equals(e.getCode())) {
                throwGraduationRequirementNotFound(
                        student,
                        primaryMajorId,
                        secondaryMajorId,
                        admissionYear
                );
            }
            throw e;
        }
    }

    // ==============================
    // Utilities
    // ==============================

    private void validateTransferStudent(Student student) {
        if (student.isTransferStudent()) {
            throw new CommonException(ErrorCode.TRANSFER_STUDENT_UNSUPPORTED);
        }
    }

    private List<Long> resolveCandidateDepartmentIds(Student student) {
        List<Long> candidateIds = new ArrayList<>();

        Department baseDepartment =
                student.getMajor() != null ? student.getMajor() : student.getDepartment();

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

    private List<Long> resolveSecondaryCandidateIds(Student student) {
        if (student.getSecondaryMajor() == null) {
            return List.of();
        }

        List<Long> candidateIds = new ArrayList<>();

        Department secondary = student.getSecondaryMajor();
        addCandidate(candidateIds, secondary.getId());

        String establishedName = secondary.getEstablishedDepartmentName();
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

    private boolean isDifferentGradRequirement(Long departmentId, int admissionYear) {
        return admissionYear == SPECIAL_YEAR
                && departmentId != null
                && SPECIAL_DEPTS.contains(departmentId);
    }

    private void throwGraduationRequirementNotFound(
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

    private record MajorResolutionResult(
            Long primaryMajorId,
            Long secondaryMajorId
    ) {
    }
}