package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
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
        Department dept = student.getDepartment();
        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long primaryMajorId = student.getMajor() != null ? student.getMajor().getId() : dept.getId();
        int admissionYear = student.getAcademicInfo().getAdmissionYear();

        List<AreaProgressDto> areaProgress = null;
        if (student.getSecondaryMajor() != null) { // 복수전공 존재
            Long secondaryMajorId = student.getSecondaryMajor().getId();
            areaProgress = graduationQueryRepository.getDualMajorAreaProgress(studentId, primaryMajorId, secondaryMajorId, admissionYear);
        } else {
            // 졸업 요건 충족 여부 조회
            areaProgress = graduationQueryRepository.getStudentAreaProgress(studentId, primaryMajorId, admissionYear);
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

    private boolean isDifferentGradRequirement(Long departmentId, int admissionYear) {
        return admissionYear == SPECIAL_YEAR && departmentId != null && SPECIAL_DEPTS.contains(departmentId);
    }
}
