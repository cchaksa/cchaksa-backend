package com.chukchuk.haksa.domain.graduation.service;

import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GraduationService {

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
            // Redis 장애 시 로그 남기고 계속 진행
            log.warn("Redis cache retrieval failed for studentId: {}", studentId, e);
        }

        Student student = studentService.getStudentById(studentId);

        // 편입생인 경우 예외 처리, TODO: 편입생 졸업 요건 추가 후 삭제
        if (student.isTransferStudent()) {
            throw new CommonException(ErrorCode.TRANSFER_STUDENT_UNSUPPORTED);
        }

        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long effectiveDepartmentId = student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId();

        // 졸업 요건 충족 여부 조회
        List<AreaProgressDto> areaProgress = graduationQueryRepository.getStudentAreaProgress(studentId, effectiveDepartmentId, student.getAcademicInfo().getAdmissionYear());

        GraduationProgressResponse response = new GraduationProgressResponse(areaProgress);

        try {
            redisCacheStore.setGraduationProgress(studentId, response);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패 - studentId: {}", studentId, e);
        }

        return response;
    }
}
