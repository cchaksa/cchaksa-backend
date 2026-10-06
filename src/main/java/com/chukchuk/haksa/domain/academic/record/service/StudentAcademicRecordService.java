package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.repository.GraduationQueryRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생의 누적 성적과 전공 구성에 맞는 졸업 필요 학점을 제공한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StudentAcademicRecordService {

  private final StudentAcademicRecordRepository studentAcademicRecordRepository;
  private final GraduationQueryRepository graduationQueryRepository;
  private final AcademicCache academicCache;

  private static final String AREA_MAJOR_ELECTIVE = "전선";
  private static final String AREA_GENERAL_ELECTIVE = "일선";

  /**
   * 학생의 누적 성적과 전공·복수전공 기준의 졸업 필요 학점을 요약한다.
   *
   * @param studentId 학생 식별자
   * @return 누적 성적과 졸업 필요 학점 요약
   * @throws EntityNotFoundException 학생의 누적 성적 기록이 없는 경우
   */
  public StudentAcademicRecordDto.AcademicSummaryResponse getAcademicSummary(UUID studentId) {
    try {
      StudentAcademicRecordDto.AcademicSummaryResponse cached =
          academicCache.getAcademicSummary(studentId);
      if (cached != null) {
        return cached;
      }
    } catch (Exception e) {
      log.warn(
          "[BIZ] academic.summary.cache.get.fail studentId={} ex={}",
          studentId,
          e.getClass().getSimpleName(),
          e);
    }

    StudentAcademicRecord studentAcademicRecord = getStudentAcademicRecordByStudentId(studentId);
    Student student = studentAcademicRecord.getStudent();

    // 전공 코드가 없는 학과도 있으므로 majorId가 없으면 departmentId를 사용
    Long effectiveDepartmentId =
        student.getMajor() != null ? student.getMajor().getId() : student.getDepartment().getId();
    Integer admissionYear = student.getAcademicInfo().getAdmissionYear();

    Long secondaryMajorId =
        student.getSecondaryMajor() != null ? student.getSecondaryMajor().getId() : null;

    Integer totalRequiredGraduationCredits =
        getGraduationCreditsWithCache(effectiveDepartmentId, secondaryMajorId, admissionYear);

    StudentAcademicRecordDto.AcademicSummaryResponse response =
        StudentAcademicRecordDto.AcademicSummaryResponse.from(
            studentAcademicRecord, totalRequiredGraduationCredits);

    try {
      academicCache.setAcademicSummary(studentId, response);
    } catch (Exception e) {
      log.warn(
          "[BIZ] academic.summary.cache.set.fail studentId={} ex={}",
          studentId,
          e.getClass().getSimpleName(),
          e);
    }

    return response;
  }

  /**
   * 학생의 누적 성적 엔티티를 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 학생에게 연결된 누적 성적
   * @throws EntityNotFoundException 학생의 누적 성적 기록이 없는 경우
   */
  public StudentAcademicRecord getStudentAcademicRecordByStudentId(UUID studentId) {
    return findStudentAcademicRecordByStudentId(studentId)
        .orElseThrow(
            () -> {
              log.warn("[BIZ] academic.summary.not_found studentId={}", studentId);
              return new EntityNotFoundException(ErrorCode.STUDENT_ACADEMIC_RECORD_NOT_FOUND);
            });
  }

  /**
   * 학생의 누적 성적 엔티티를 예외 없이 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 학생에게 연결된 누적 성적이 있으면 반환
   */
  public Optional<StudentAcademicRecord> findStudentAcademicRecordByStudentId(UUID studentId) {
    return studentAcademicRecordRepository.findByStudentId(studentId);
  }

  /** 복수 전공을 고려한 졸업 필요 학점 계산 메서드. */
  private Integer getGraduationCreditsWithCache(
      Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
    // 단일 전공 케이스
    if (secondaryMajorId == null) {
      return graduationQueryRepository
          .getAreaRequirementsWithCache(primaryMajorId, admissionYear)
          .stream()
          .mapToInt(AreaRequirementDto::requiredCredits)
          .sum();
    }

    // 복수전공 케이스
    int primaryCredits =
        graduationQueryRepository
            .getAreaRequirementsWithCache(primaryMajorId, admissionYear)
            .stream()
            .filter(req -> !req.areaType().equalsIgnoreCase(AREA_MAJOR_ELECTIVE))
            .filter(req -> !req.areaType().equalsIgnoreCase(AREA_GENERAL_ELECTIVE))
            .mapToInt(AreaRequirementDto::requiredCredits)
            .sum();

    int dualCredits =
        graduationQueryRepository
            .getDualMajorRequirementsWithCache(primaryMajorId, secondaryMajorId, admissionYear)
            .stream()
            .mapToInt(AreaRequirementDto::requiredCredits)
            .sum();

    int totalCredits = Math.max(130, primaryCredits + dualCredits);
    return totalCredits;
  }
}
