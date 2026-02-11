package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.policy.GraduationMajorResolver;
import com.chukchuk.haksa.domain.graduation.policy.MajorResolutionResult;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GraduationService {

    private static final int SPECIAL_YEAR = 2025;
    private static final Set<Long> SPECIAL_DEPTS = Set.of(
            30L,   // 건축도시부동산학부
            115L,  // 아트앤엔터테인먼트학부
            127L   // 디자인학부
    );

    private final StudentService studentService;
    private final GraduationMajorResolver graduationMajorResolver;
    private final GraduationQueryRepository graduationQueryRepository;
    private final AcademicCache academicCache;

    /* 졸업 요건 진행 상황 조회 */
    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        // 1. 캐시 조회
        try {
            GraduationProgressResponse cached = academicCache.getGraduationProgress(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn(
                    "[BIZ] graduation.progress.cache.get.fail studentId={} ex={}",
                    studentId,
                    e.getClass().getSimpleName(),
                    e
            );
        }

        Student student = studentService.getStudentById(studentId);
        validateTransferStudent(student);

        int admissionYear = student.getAcademicInfo().getAdmissionYear();

        MajorResolutionResult majorResolution =
                graduationMajorResolver.resolve(student, admissionYear);

        List<AreaProgressDto> areaProgress =
                resolveAreaProgress(
                        student,
                        studentId,
                        majorResolution.primaryMajorId(),
                        majorResolution.secondaryMajorId(),
                        admissionYear
                );

        GraduationProgressResponse response =
                new GraduationProgressResponse(areaProgress);

        // 5. 특이 졸업 요건 여부 표시
        if (isDifferentGradRequirement(
                majorResolution.primaryMajorId(),
                admissionYear
        )) {
            response.setHasDifferentGraduationRequirement();
        }

        // 6. 캐시 저장
        try {
            academicCache.setGraduationProgress(studentId, response);
        } catch (Exception e) {
            log.warn(
                    "[BIZ] graduation.progress.cache.set.fail studentId={} ex={}",
                    studentId,
                    e.getClass().getSimpleName(),
                    e
            );
        }

        return response;
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
                    student,
                    studentId,
                    primaryMajorId,
                    admissionYear
            );
        }

        return getDualMajorProgressOrThrow(
                student,
                studentId,
                primaryMajorId,
                secondaryMajorId,
                admissionYear
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
                        studentId,
                        departmentId,
                        admissionYear
                );

        if (result.isEmpty()) {
            throw new CommonException(
                    ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND
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
            if (ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND
                    .code()
                    .equals(e.getCode())) {
                throw e;
            }
            throw e;
        }
    }

    // ==============================
    // Utilities
    // ==============================

    private void validateTransferStudent(Student student) {
        if (student.isTransferStudent()) {
            throw new CommonException(
                    ErrorCode.TRANSFER_STUDENT_UNSUPPORTED
            );
        }
    }

    private boolean isDifferentGradRequirement(
            Long departmentId,
            int admissionYear
    ) {
        return admissionYear == SPECIAL_YEAR
                && departmentId != null
                && SPECIAL_DEPTS.contains(departmentId);
    }
}