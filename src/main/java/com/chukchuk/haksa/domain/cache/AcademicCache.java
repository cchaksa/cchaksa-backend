package com.chukchuk.haksa.domain.cache;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.student.dto.StudentSemesterDto;
import java.util.List;
import java.util.UUID;

/** 학생·학과별 학사 조회 결과를 캐시하는 계약을 정의한다. */
public interface AcademicCache {

  /**
   * 학생의 누적 학사 요약을 캐시한다.
   *
   * @param studentId 학생 식별자
   * @param summary 캐시할 누적 학사 요약
   */
  void setAcademicSummary(UUID studentId, AcademicSummaryResponse summary);

  /**
   * 학생의 누적 학사 요약을 캐시에서 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 캐시된 학사 요약, 없으면 {@code null}
   */
  AcademicSummaryResponse getAcademicSummary(UUID studentId);

  /**
   * 학생의 학기 목록을 캐시한다.
   *
   * @param studentId 학생 식별자
   * @param list 캐시할 학기 목록
   */
  void setSemesterList(UUID studentId, List<StudentSemesterDto.StudentSemesterInfoResponse> list);

  /**
   * 학생의 학기 목록을 캐시에서 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 캐시된 학기 목록, 없으면 {@code null}
   */
  List<StudentSemesterDto.StudentSemesterInfoResponse> getSemesterList(UUID studentId);

  /**
   * 학생의 졸업 요건 진행 상태를 캐시한다.
   *
   * @param studentId 학생 식별자
   * @param progress 캐시할 졸업 요건 진행 상태
   */
  void setGraduationProgress(UUID studentId, GraduationProgressResponse progress);

  /**
   * 학생의 졸업 요건 진행 상태를 캐시에서 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 캐시된 졸업 진행 상태, 없으면 {@code null}
   */
  GraduationProgressResponse getGraduationProgress(UUID studentId);

  /**
   * 학과와 입학 연도에 해당하는 졸업 영역별 요건을 캐시한다.
   *
   * @param departmentId 학과 식별자
   * @param admissionYear 입학 연도
   * @param requirements 캐시할 영역별 졸업 요건
   */
  void setGraduationRequirements(
      Long departmentId, Integer admissionYear, List<AreaRequirementDto> requirements);

  /**
   * 학과와 입학 연도의 졸업 영역별 요건을 캐시에서 조회한다.
   *
   * @param departmentId 학과 식별자
   * @param admissionYear 입학 연도
   * @return 캐시된 영역별 졸업 요건, 없으면 {@code null}
   */
  List<AreaRequirementDto> getGraduationRequirements(Long departmentId, Integer admissionYear);

  /**
   * 주전공·복수전공·입학 연도 조합의 졸업 요건을 캐시한다.
   *
   * @param primaryMajorId 주전공 식별자
   * @param secondaryMajorId 복수전공 식별자
   * @param admissionYear 입학 연도
   * @param requirements 캐시할 복수전공 졸업 요건
   */
  void setDualMajorRequirements(
      Long primaryMajorId,
      Long secondaryMajorId,
      Integer admissionYear,
      List<AreaRequirementDto> requirements);

  /**
   * 주전공·복수전공·입학 연도 조합의 졸업 요건을 캐시에서 조회한다.
   *
   * @param primaryMajorId 주전공 식별자
   * @param secondaryMajorId 복수전공 식별자
   * @param admissionYear 입학 연도
   * @return 캐시된 복수전공 졸업 요건, 없으면 {@code null}
   */
  List<AreaRequirementDto> getDualMajorRequirements(
      Long primaryMajorId, Long secondaryMajorId, Integer admissionYear);

  /**
   * 학생의 학기별 성적 요약을 캐시한다.
   *
   * @param studentId 학생 식별자
   * @param list 캐시할 학기별 성적 요약
   */
  void setSemesterSummaries(UUID studentId, List<SemesterSummaryResponse> list);

  /**
   * 학생의 학기별 성적 요약을 캐시에서 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 캐시된 학기별 성적 요약, 없으면 {@code null}
   */
  List<SemesterSummaryResponse> getSemesterSummaries(UUID studentId);

  /**
   * 학생 식별자로 시작하는 모든 학사 캐시를 제거한다.
   *
   * @param studentId 학생 식별자
   */
  void deleteAllByStudentId(UUID studentId);
}
