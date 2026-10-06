package com.chukchuk.haksa.domain.academic.record.service;

import static com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto.SemesterGradeResponse;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생의 학기별 성적과 학기 목록을 조회하고 캐시한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SemesterAcademicRecordService {
  private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
  private final AcademicCache academicCache;

  private static final Duration SEMESTER_RECORD_TTL = Duration.ofMinutes(3);

  /* 특정 학생의 특정 학기 성적 조회 */
  /**
   * 학생의 특정 학기 성적을 조회한다.
   *
   * @param studentId 학생 식별자
   * @param year 연도
   * @param semester 조회할 학기
   * @return 해당 학기의 성적 정보
   * @throws EntityNotFoundException 해당 학기 성적이 없는 경우
   */
  public SemesterGradeResponse getSemesterGradesByYearAndSemester(
      UUID studentId, Integer year, Integer semester) {
    SemesterAcademicRecord records =
        semesterAcademicRecordRepository
            .findByStudentIdAndYearAndSemester(studentId, year, semester)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[BIZ] academic.semester.record.not_found studentId={} year={} semester={}",
                      studentId,
                      year,
                      semester);
                  return new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_NOT_FOUND);
                });

    return SemesterGradeResponse.from(records);
  }

  /* 특정 학생의 전체 학기 성적 조회 (최신순 정렬) */
  /**
   * 학생의 전체 학기 성적을 최신 학기부터 반환한다.
   *
   * @param studentId 학생 식별자
   * @return 최신순으로 정렬된 학기별 성적 요약
   * @throws EntityNotFoundException 저장된 학기 성적이 하나도 없는 경우
   */
  public List<SemesterSummaryResponse> getAllSemesterGrades(UUID studentId) {
    List<SemesterSummaryResponse> records = getSemesterSummaries(studentId);

    if (records.isEmpty()) {
      log.warn("[BIZ] academic.semester.records.empty studentId={}", studentId);
      throw new EntityNotFoundException(ErrorCode.SEMESTER_RECORD_EMPTY);
    }

    return records;
  }

  /* 학생의 학기 정보 조회 */
  /**
   * 학생의 성적이 존재하는 학기 목록을 반환한다.
   *
   * @param studentId 학생 식별자
   * @return 성적 조회에 사용할 학기 목록
   * @throws CommonException 신입생 등 조회할 학기가 없는 경우
   */
  public List<StudentSemesterDto.StudentSemesterInfoResponse> getSemestersByStudentId(
      UUID studentId) {

    List<SemesterSummaryResponse> records = getSemesterSummaries(studentId);

    if (records.isEmpty()) {
      log.warn("[BIZ] academic.semester.freshman_no_semester studentId={}", studentId);
      throw new CommonException(ErrorCode.FRESHMAN_NO_SEMESTER);
    }

    return records.stream()
        .map(StudentSemesterDto.StudentSemesterInfoResponse::from)
        .collect(Collectors.toList());
  }

  /**
   * 학생의 학기별 성적 요약을 캐시 우선으로 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 최신순으로 정렬된 학기별 성적 요약, 성적이 없으면 빈 목록
   */
  public List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId) {
    try {
      List<SemesterSummaryResponse> cached = academicCache.getSemesterSummaries(studentId);
      if (cached != null) {
        return cached;
      }
    } catch (Exception e) {
      log.warn(
          "[BIZ] semester.summaries.cache.get.fail studentId={} ex={}",
          studentId,
          e.getClass().getSimpleName(),
          e);
    }

    List<SemesterAcademicRecord> records =
        semesterAcademicRecordRepository.findByStudentIdOrderByYearDescSemesterDesc(studentId);

    List<SemesterSummaryResponse> summaries =
        records.stream().map(SemesterSummaryResponse::from).toList();

    if (records.isEmpty()) {
      log.warn("[BIZ] semester.summaries.empty studentId={}", studentId);
      return summaries;
    }

    try {
      academicCache.setSemesterSummaries(studentId, summaries);
    } catch (Exception e) {
      log.warn(
          "[BIZ] semester.summaries.cache.set.fail studentId={} ex={}",
          studentId,
          e.getClass().getSimpleName(),
          e);
    }

    return summaries;
  }
}
