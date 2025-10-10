package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto.SemesterGradeResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SemesterAcademicRecordService {
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
    private final RedisCacheStore redisCacheStore;

    private static final Duration SEMESTER_RECORD_TTL = Duration.ofMinutes(3);

    /* 특정 학생의 특정 학기 성적 조회 */
    public SemesterGradeResponse getSemesterGradesByYearAndSemester(UUID studentId, Integer year, Integer semester) {
        SemesterAcademicRecord records = semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, year, semester)
                .orElseThrow(() -> {
                    log.warn("[BIZ] academic.semester.record.not_found studentId={} year={} semester={}",
                            studentId, year, semester);
                    return new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_NOT_FOUND);
                });

        return SemesterGradeResponse.from(records);
    }

    /* 특정 학생의 전체 학기 성적 조회 (최신순 정렬) */
    public List<SemesterSummaryResponse> getAllSemesterGrades(UUID studentId) {
        List<SemesterSummaryResponse> records = getSemesterSummaries(studentId);

        if (records.isEmpty()) {
            log.warn("[BIZ] academic.semester.records.empty studentId={}", studentId);
            throw new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_EMPTY);
        }

        return records;
    }

    /* 학생의 학기 정보 조회 */
    public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemestersByStudentId(UUID studentId) {

        List<SemesterSummaryResponse> records = getSemesterSummaries(studentId);

        if (records.isEmpty()) {
            log.warn("[BIZ] academic.semester.freshman_no_semester studentId={}", studentId);
            throw new CommonException(ErrorCode.FRESHMAN_NO_SEMESTER);
        }

        return records.stream()
                .map(StudentSemesterDto.StudentSemesterInfoResponse::from)
                .collect(Collectors.toList());
    }

    public List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId) {
        try {
            List<SemesterSummaryResponse> cached = redisCacheStore.getSemesterSummaries(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] semester.summaries.cache.get.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        List<SemesterAcademicRecord> records = semesterAcademicRecordRepository
                .findByStudentIdOrderByYearDescSemesterDesc(studentId);

        List<SemesterSummaryResponse> summaries = records.stream()
                .map(SemesterSummaryResponse::from)
                .toList();

        if (records.isEmpty()) {
            log.warn("[BIZ] semester.summaries.empty studentId={}", studentId);
            return summaries;
        }

        try {
            redisCacheStore.setSemesterSummaries(studentId, summaries, Duration.ofMinutes(3));
        } catch (Exception e) {
            log.warn("[BIZ] semester.summaries.cache.set.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        return summaries;
    }

}
