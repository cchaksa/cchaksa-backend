package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.util.HashUtil;
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
        String userHash = HashUtil.sha256Short(studentId.toString());
        try {
            StudentAcademicRecordDto.AcademicSummaryResponse cached = redisCacheStore.getAcademicSummary(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] academic.summary.cache.get.fail userIdHash={} ex={}", userHash, e.getClass().getSimpleName(), e);
        }

        StudentAcademicRecord studentAcademicRecord = getStudentAcademicRecordByStudentId(studentId);

        Student student = studentService.getStudentById(studentId);

        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long effectiveDepartmentId = student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId();
        Integer admissionYear = student.getAcademicInfo().getAdmissionYear();

        Integer totalRequiredGraduationCredits = graduationQueryRepository.getTotalRequiredGraduationCredits(effectiveDepartmentId, admissionYear);

        StudentAcademicRecordDto.AcademicSummaryResponse response = StudentAcademicRecordDto.AcademicSummaryResponse.from(studentAcademicRecord, totalRequiredGraduationCredits);

        try {
            redisCacheStore.setAcademicSummary(studentId, response);
        } catch (Exception e) {
            log.warn("[BIZ] academic.summary.cache.set.fail userIdHash={} ex={}", userHash, e.getClass().getSimpleName(), e);
        }
        return response;
    }

    public StudentAcademicRecord getStudentAcademicRecordByStudentId(UUID studentId) {
        return studentAcademicRecordRepository.findByStudentId(studentId)
                .orElseThrow(() -> {
                    String userHash = HashUtil.sha256Short(studentId.toString());
                    log.warn("[BIZ] academic.summary.not_found userIdHash={}", userHash);
                    return new EntityNotFoundException(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND);
                });
    }
}
