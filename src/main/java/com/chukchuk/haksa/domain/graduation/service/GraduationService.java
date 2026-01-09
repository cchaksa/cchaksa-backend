package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
    private final GraduationQueryRepository graduationQueryRepository;
    private final RedisCacheStore redisCacheStore;

    /* 졸업 요건 진행 상황 조회 */
    public GraduationProgressResponse getGraduationProgress(UUID studentId) {
        try {
            GraduationProgressResponse cached = redisCacheStore.getGraduationProgress(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] graduation.progress.cache.get.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        Student student = studentService.getStudentById(studentId);
        // 편입생인 경우 예외 처리, TODO: 편입생 졸업 요건 추가 후 삭제
        validateTransferStudent(student);

        Department dept = student.getDepartment();
        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long primaryMajorId = student.getMajor() != null ? student.getMajor().getId() : dept.getId();
        int admissionYear = student.getAcademicInfo().getAdmissionYear();

        List<AreaProgressDto> areaProgress;

        if (student.getSecondaryMajor() != null) {
            areaProgress = getDualMajorProgressOrThrow(
                    student, studentId, primaryMajorId, admissionYear
            );
        } else {
            areaProgress = getSingleMajorProgressOrThrow(
                    student, studentId, primaryMajorId, admissionYear
            );
        }

        GraduationProgressResponse response = new GraduationProgressResponse(areaProgress);

        if (isDifferentGradRequirement(primaryMajorId, admissionYear)) {
            response.setHasDifferentGraduationRequirement();
            log.info("[BIZ] graduation.progress.flag.set studentId={} deptId={} year={}", studentId, primaryMajorId, admissionYear);
        }

        try {
            redisCacheStore.setGraduationProgress(studentId, response);
        } catch (Exception e) {
            log.warn("[BIZ] graduation.progress.cache.set.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        return response;
    }

    // 편입생인 경우 예외 처리, TODO: 편입생 졸업 요건 추가 후 삭제
    private void validateTransferStudent(Student student) {
        if (student.isTransferStudent()) {
            throw new CommonException(ErrorCode.TRANSFER_STUDENT_UNSUPPORTED);
        }
    }

    private boolean isDifferentGradRequirement(Long departmentId, int admissionYear) {
        return admissionYear == SPECIAL_YEAR && departmentId != null && SPECIAL_DEPTS.contains(departmentId);
    }

    // 단일 전공 처리 메서드
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
                    admissionYear);
        }

        return result;
    }

    // 복수 전공 처리 메서드
    // 복수 전공 처리 메서드
    private List<AreaProgressDto> getDualMajorProgressOrThrow(
            Student student,
            UUID studentId,
            Long primaryMajorId,
            int admissionYear
    ) {
        Long secondaryMajorId = student.getSecondaryMajor().getId();

        try {
            return graduationQueryRepository.getDualMajorAreaProgress(
                    studentId, primaryMajorId, secondaryMajorId, admissionYear
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

    /**
     * 졸업 요건 부재 예외 처리 메서드
     * MDC 정리는 GlobalExceptionHandler에서 수행됨
     */
    private void throwGraduationRequirementNotFound(
            Student student,
            Long primaryMajorId,
            Long secondaryMajorId,
            int admissionYear
    ) {
        MDC.put("student_code", student.getStudentCode());
        MDC.put("admission_year", String.valueOf(admissionYear));
        MDC.put("primary_department_id", String.valueOf(primaryMajorId));

        if (secondaryMajorId == null) {
            MDC.put("major_type", "SINGLE");
            MDC.put("secondary_department_id", "NONE");
        } else {
            MDC.put("major_type", "DUAL");
            MDC.put("secondary_department_id", String.valueOf(secondaryMajorId));
        }

        throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND);
    }
}
