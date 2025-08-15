package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.exception.ReconnectionRequiredException;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StudentAcademicRecordService {

    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final GraduationQueryRepository graduationQueryRepository;
    private final StudentService studentService;
    private final RedisCacheStore redisCacheStore;

    public StudentAcademicRecordDto.AcademicSummaryResponse getAcademicSummary(UUID studentId) {
        /**
         * 사용자 재연동 처리 로직 추가
         * Students.reconnection == false && Users.portal_connected == true -> 재연동이 필요한 사용자
         * 재연동 후 Students.reconnection = true로 변경
          */
        Student student = studentService.getStudentById(studentId);

        // 재연동 체크: 캐시 접근 전에 진입
        if (!student.isReconnection() && student.getUser().getPortalConnected()) {
            log.info("재연동이 필요한 사용자 - studentId: {}, userId: {}", studentId, student.getUser().getId());
            throw new ReconnectionRequiredException();
        }


        try {
            StudentAcademicRecordDto.AcademicSummaryResponse cached = redisCacheStore.getAcademicSummary(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            // Redis 장애 시 로그 남기고 계속 진행
            log.warn("Redis cache retrieval failed for studentId: {}", studentId, e);
        }

        StudentAcademicRecord studentAcademicRecord = getStudentAcademicRecordByStudentId(studentId);

        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long effectiveDepartmentId = student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId();
        Integer admissionYear = student.getAcademicInfo().getAdmissionYear();

        Integer totalRequiredGraduationCredits = graduationQueryRepository.getTotalRequiredGraduationCredits(effectiveDepartmentId, admissionYear);

        StudentAcademicRecordDto.AcademicSummaryResponse response = StudentAcademicRecordDto.AcademicSummaryResponse.from(studentAcademicRecord, totalRequiredGraduationCredits);

        try {
            redisCacheStore.setAcademicSummary(studentId, response);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패 - studentId: {}", studentId, e);
        }

        return response;
    }

    public StudentAcademicRecord getStudentAcademicRecordByStudentId(UUID studentId) {
        return studentAcademicRecordRepository.findByStudentId(studentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND));
    }
}
