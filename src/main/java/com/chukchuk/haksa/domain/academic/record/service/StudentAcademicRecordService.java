package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
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
public class StudentAcademicRecordService {

    private final StudentAcademicRecordRepository studentAcademicRecordRepository;
    private final GraduationQueryRepository graduationQueryRepository;
    private final StudentService studentService;
    private final RedisCacheStore redisCacheStore;

    public StudentAcademicRecordDto.AcademicSummaryResponse getAcademicSummary(UUID studentId) {
        try {
            StudentAcademicRecordDto.AcademicSummaryResponse cached = redisCacheStore.getAcademicSummary(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] academic.summary.cache.get.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        StudentAcademicRecord studentAcademicRecord = getStudentAcademicRecordByStudentId(studentId);
        Student student = studentAcademicRecord.getStudent();

        // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
        Long effectiveDepartmentId = student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId();
        Integer admissionYear = student.getAcademicInfo().getAdmissionYear();

        Integer totalRequiredGraduationCredits = getGraduationCreditsWithCache(effectiveDepartmentId, admissionYear);

        StudentAcademicRecordDto.AcademicSummaryResponse response = StudentAcademicRecordDto.AcademicSummaryResponse.from(studentAcademicRecord, totalRequiredGraduationCredits);

        try {
            redisCacheStore.setAcademicSummary(studentId, response);
        } catch (Exception e) {
            log.warn("[BIZ] academic.summary.cache.set.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        return response;
    }

    public StudentAcademicRecord getStudentAcademicRecordByStudentId(UUID studentId) {
        return studentAcademicRecordRepository.findByStudentId(studentId)
                .orElseThrow(() -> {
                    log.warn("[BIZ] academic.summary.not_found studentId={}", studentId);
                    return new EntityNotFoundException(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND);
                });
    }

    private Integer getGraduationCreditsWithCache(Long deptId, Integer admissionYear) {
        try {
            List<AreaRequirementDto> requirements = redisCacheStore.getGraduationRequirements(deptId, admissionYear);
            if (requirements != null && !requirements.isEmpty()) {
                return requirements.stream()
                        .mapToInt(AreaRequirementDto::requiredCredits)
                        .sum();
            }

            requirements = graduationQueryRepository.getAreaRequirements(deptId, admissionYear);
            redisCacheStore.setGraduationRequirements(deptId, admissionYear, requirements);

            return requirements.stream()
                    .mapToInt(AreaRequirementDto::requiredCredits)
                    .sum();

        } catch (Exception e) {
            log.warn("[BIZ] academic.summary.graduation_credits.cache.fail deptId={} year={} ex={}",
                    deptId, admissionYear, e.getClass().getSimpleName());
            return graduationQueryRepository.getAreaRequirements(deptId, admissionYear).stream()
                    .mapToInt(AreaRequirementDto::requiredCredits)
                    .sum();
        }
    }
}
