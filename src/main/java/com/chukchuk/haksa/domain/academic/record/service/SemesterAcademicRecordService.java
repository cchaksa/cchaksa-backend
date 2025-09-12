package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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

    /* 특정 학생의 특정 학기 성적 조회 */
    public SemesterGradeResponse getSemesterGradesByYearAndSemester(UUID studentId, Integer year, Integer semester) {
        return SemesterGradeResponse.from(findSemesterRecordsByYearAndSemester(studentId, year, semester));
    }

    /* 특정 학생의 전체 학기 성적 조회 (최신순 정렬) */
    public List<SemesterGradeResponse> getAllSemesterGrades(UUID studentId) {
        return findAllSemesterRecords(studentId).stream()
                .map(SemesterGradeResponse::from)
                .collect(Collectors.toList());
    }

    /* 특정 학생의 특정 학기 성적 조회 (없으면 예외 발생) */
    private SemesterAcademicRecord findSemesterRecordsByYearAndSemester(UUID studentId, Integer year, Integer semester) {
        return semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, year, semester)
                .orElseThrow(() -> {
                    log.warn("[BIZ] academic.semester.record.not_found studentId={} year={} semester={}",
                            studentId, year, semester);
                    return new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_NOT_FOUND);
                });
    }

    /* 특정 학생의 전체 학기 성적 조회 (최신순 정렬, 없으면 예외 발생) */
    private List<SemesterAcademicRecord> findAllSemesterRecords(UUID studentId) {
        List<SemesterAcademicRecord> records =
                semesterAcademicRecordRepository.findByStudentIdOrderByYearDescSemesterDesc(studentId);
        if (records.isEmpty()) {
            log.warn("[BIZ] academic.semester.records.empty studentId={}", studentId);
            throw new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_EMPTY);
        }
        return records;
    }

    /* 학생의 학기 정보 조회 */
    public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemestersByStudentId(UUID studentId) {
        try {
            List<StudentSemesterDto.StudentSemesterInfoResponse> cached = redisCacheStore.getSemesterList(studentId);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[BIZ] academic.semester.cache.get.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }

        List<StudentSemesterDto.StudentSemesterInfoResponse> response = findSemestersByStudent(studentId).stream()
                .sorted(Comparator
                        .comparing(SemesterAcademicRecord::getYear).reversed()
                        .thenComparing(SemesterAcademicRecord::getSemester, Comparator.reverseOrder()))
                .map(StudentSemesterDto.StudentSemesterInfoResponse::from)
                .collect(Collectors.toList());

        try {
            redisCacheStore.setSemesterList(studentId, response);
        } catch (Exception e) {
            log.warn("[BIZ] academic.semester.cache.set.fail studentId={} ex={}", studentId, e.getClass().getSimpleName(), e);
        }
        return response;
    }

    /* 특정 학생의 학기 정보 조회 (신입생 예외 처리) */
    private List<SemesterAcademicRecord> findSemestersByStudent(UUID studentId) {
        List<SemesterAcademicRecord> records = semesterAcademicRecordRepository.findByStudentId(studentId);
        if (records.isEmpty()) {
            log.warn("[BIZ] academic.semester.freshman_no_semester studentId={}", studentId);
            throw new CommonException(ErrorCode.FRESHMAN_NO_SEMESTER);
        }
        return records;
    }

}
