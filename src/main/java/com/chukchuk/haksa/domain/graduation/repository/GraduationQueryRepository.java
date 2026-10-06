package com.chukchuk.haksa.domain.graduation.repository;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/** 졸업 query 데이터 조회와 저장 기능을 제공한다. */
@Repository
@RequiredArgsConstructor
@Slf4j
public class GraduationQueryRepository {
  private final EntityManager em;
  private final AcademicCache academicCache;

  private static final String AREA_MAJOR_ELECTIVE = "전선"; // 전공선택
  private static final String AREA_GENERAL_ELECTIVE = "일선"; // 일반선택
  private static final String AREA_ETC = FacultyDivision.기타.name();
  private static final int ETC_REQUIRED_CREDITS = 0;

  /* 졸업 요건 조회 (학과 코드, 입학년도) */
  /**
   * 학과와 입학 연도에 적용되는 영역별 졸업 요건을 조회한다.
   *
   * @param departmentId 학과 식별자
   * @param admissionYear 입학 연도
   * @return 영역별 졸업 요건 목록
   */
  public List<AreaRequirementDto> getAreaRequirements(Long departmentId, Integer admissionYear) {
    String sql =
        """
        SELECT
            dar.area_type,
            dar.required_credits,
            dar.required_elective_courses,
            dar.total_elective_courses
        FROM department_area_requirements dar
        WHERE dar.department_id = :departmentId
          AND dar.admission_year = :admissionYear
        """;

    Query query = em.createNativeQuery(sql);
    query.setParameter("departmentId", departmentId);
    query.setParameter("admissionYear", admissionYear);

    List<Object[]> results = query.getResultList();

    return results.stream()
        .map(
            row ->
                new AreaRequirementDto(
                    (String) row[0], // area_type
                    toInteger(row[1]), // required_credits
                    toInteger(row[2]), // required_elective_courses (nullable)
                    toInteger(row[3]) // total_elective_courses (nullable)
                    ))
        .toList();
  }

  /* 복수 전공 졸업 요건 조회 (학과 코드, 입학년도) */
  private List<AreaRequirementDto> getDualMajorRequirements(
      Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
    String sql =
        """
        SELECT
            dmr.area_type,
            dmr.required_credits,
            NULL AS required_elective_courses,
            NULL AS total_elective_courses
        FROM dual_major_requirements dmr
        WHERE ((dmr.department_id = :primaryId AND dmr.major_role = 'PRIMARY')
            OR (dmr.department_id = :secondaryId AND dmr.major_role = 'SECONDARY'))
          AND dmr.admission_year = :admissionYear
        """;

    Query query = em.createNativeQuery(sql);
    query.setParameter("primaryId", primaryMajorId);
    query.setParameter("secondaryId", secondaryMajorId);
    query.setParameter("admissionYear", admissionYear);

    List<Object[]> results = query.getResultList();

    return results.stream()
        .map(
            row ->
                new AreaRequirementDto(
                    (String) row[0], toInteger(row[1]), toInteger(row[2]), toInteger(row[3])))
        .toList();
  }

  /**
   * 학생의 이수 과목을 영역별 졸업 요건과 비교해 진행 상태를 조회한다.
   *
   * @param studentId 학생 식별자
   * @param departmentId 학과 식별자
   * @param admissionYear 입학 연도
   * @return 영역별 이수 진행 상태 목록
   */
  public List<AreaProgressDto> getStudentAreaProgress(
      UUID studentId, Long departmentId, Integer admissionYear) {
    final long t0 = LogTime.start();

    List<AreaRequirementDto> areaRequirements =
        getAreaRequirementsWithCache(departmentId, admissionYear);
    List<CourseInternalDto> completedCourses = getLatestValidCourses(studentId);

    Map<String, List<CourseInternalDto>> coursesByArea = groupCoursesByArea(completedCourses);

    List<AreaProgressDto> result = new ArrayList<>();

    for (AreaRequirementDto req : areaRequirements) {
      String areaType = normalizeAreaType(req.areaType());
      List<CourseInternalDto> taken = coursesByArea.getOrDefault(areaType, Collections.emptyList());

      int earnedCredits =
          taken.stream()
              .mapToInt(course -> course.getCredits() != null ? course.getCredits() : 0)
              .sum();
      int completedElectiveCourses = countCompletedElectiveCourses(areaType, taken);

      List<CourseDto> courseDtos = taken.stream().map(this::toCourseResponseDto).toList();

      AreaProgressDto dto =
          new AreaProgressDto(
              parseDivision(areaType),
              req.requiredCredits(),
              earnedCredits,
              req.requiredElectiveCourses(),
              completedElectiveCourses,
              req.totalElectiveCourses(),
              courseDtos);
      result.add(dto);
    }

    appendEtcAreaIfPresent(result, coursesByArea);

    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info(
          "[BIZ] graduation.progress.query.done studentId={} deptId={} "
              + "admissionYear={} rows={} took_ms={}",
          studentId,
          departmentId,
          admissionYear,
          result.size(),
          tookMs);
    }

    return result;
  }

  /**
   * 주전공의 전공 기초 교양과 과목이 겹치는 경우 테스트 필요 주전공 기존 전선 졸업 요건 -> 복수전공용 전선1로 대체 복수전공 졸업 요건 영역: 전교, 전필, 전선.
   *
   * @param studentId 졸업 이수 현황을 계산할 학생 식별자
   * @param primaryMajorId 주전공 학과 식별자
   * @param secondaryMajorId 복수전공 학과 식별자
   * @param admissionYear 졸업 요건 적용 기준 입학 연도
   * @return 주전공과 복수전공 요건을 함께 적용한 영역별 이수 현황
   * @throws CommonException 주전공 또는 복수전공 졸업 요건 데이터가 없는 경우
   */
  public List<AreaProgressDto> getDualMajorAreaProgress(
      UUID studentId, Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
    final long t0 = LogTime.start();

    // 주전공 졸업 요건 조회
    List<AreaRequirementDto> primaryReqs =
        getAreaRequirementsWithCache(primaryMajorId, admissionYear);

    // 주전공 졸업 요건 데이터 부재 시 404 예외 처리
    if (primaryReqs == null || primaryReqs.isEmpty()) {
      throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND);
    }

    // 주전공 졸업 요건 중 전선/일선 제외
    List<AreaRequirementDto> primaryFiltered =
        primaryReqs.stream()
            .filter(req -> !req.areaType().equalsIgnoreCase(AREA_MAJOR_ELECTIVE))
            .filter(req -> !req.areaType().equalsIgnoreCase(AREA_GENERAL_ELECTIVE))
            .toList();

    // 필터링 후 빈 리스트가 되는 경우를 대비한 방어 코드
    if (primaryFiltered.isEmpty()) {
      throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND);
    }

    // 복수전공 졸업 요건 조회
    List<AreaRequirementDto> dualMajorReqs =
        getDualMajorRequirementsWithCache(primaryMajorId, secondaryMajorId, admissionYear);

    // 복수 전공 졸업 요건 데이터 부재 시 404 예외 처리
    if (dualMajorReqs == null || dualMajorReqs.isEmpty()) {
      throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_DATA_NOT_FOUND);
    }

    // 전체 병합
    List<AreaRequirementDto> mergedRequirements = new ArrayList<>();
    mergedRequirements.addAll(primaryFiltered); // 주전공 졸업 요건 (전선 제외)
    mergedRequirements.addAll(dualMajorReqs); // 복수전공용 졸업 요건

    int totalRequiredCreditsExcluding =
        mergedRequirements.stream().mapToInt(AreaRequirementDto::requiredCredits).sum();

    int ilsunRequired =
        (totalRequiredCreditsExcluding < 130) ? 130 - totalRequiredCreditsExcluding : 0;

    mergedRequirements.add(
        new AreaRequirementDto(AREA_GENERAL_ELECTIVE, ilsunRequired, null, null));

    // 수강 이력 조회
    List<CourseInternalDto> completedCourses = getLatestValidCourses(studentId);
    Map<String, List<CourseInternalDto>> coursesByArea = groupCoursesByArea(completedCourses);

    // 이수 현황 계산
    List<AreaProgressDto> result = new ArrayList<>();
    for (AreaRequirementDto req : mergedRequirements) {
      String areaType = normalizeAreaType(req.areaType());
      List<CourseInternalDto> taken = coursesByArea.getOrDefault(areaType, Collections.emptyList());

      int earnedCredits =
          taken.stream()
              .mapToInt(course -> course.getCredits() != null ? course.getCredits() : 0)
              .sum();
      int completedElectiveCourses = countCompletedElectiveCourses(areaType, taken);

      List<CourseDto> courseDtos = taken.stream().map(this::toCourseResponseDto).toList();

      AreaProgressDto dto =
          new AreaProgressDto(
              parseDivision(areaType),
              req.requiredCredits(),
              earnedCredits,
              req.requiredElectiveCourses(),
              completedElectiveCourses,
              req.totalElectiveCourses(),
              courseDtos);

      result.add(dto);
    }

    appendEtcAreaIfPresent(result, coursesByArea);

    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info(
          "[BIZ] graduation.dual.progress.query.done studentId={} primaryDept={} "
              + "secondaryDept={} year={} rows={} took_ms={}",
          studentId,
          primaryMajorId,
          secondaryMajorId,
          admissionYear,
          result.size(),
          tookMs);
    }

    return result;
  }

  /**
   * 재수강·실패 과목을 제외하고 과목별 최신 유효 이수 내역을 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 과목별 최신 유효 이수 내역 목록
   */
  public List<CourseInternalDto> getLatestValidCourses(UUID studentId) {
    String sql =
        """
            SELECT DISTINCT ON (c.course_code, co.faculty_division_name)
                sc.offering_id,
                TRIM(co.faculty_division_name) AS area_type,
                sc.points,
                sc.grade,
                c.course_name,
                co.semester,
                co.year,
                c.course_code,
                sc.original_score,
                co.area_code
            FROM student_courses sc
            JOIN course_offerings co ON sc.offering_id = co.id
            JOIN courses c ON co.course_id = c.id
            WHERE sc.grade NOT IN ('F', 'R')
              AND sc.student_id = :studentId
              AND sc.is_retake_deleted = FALSE
            ORDER BY c.course_code, co.faculty_division_name, co.year DESC, co.semester DESC, sc.original_score DESC
        """;

    Query query = em.createNativeQuery(sql);
    query.setParameter("studentId", studentId);

    List<Object[]> rows = query.getResultList();

    return rows.stream()
        .map(
            r ->
                new CourseInternalDto(
                    (Long) r[0], // offering_id
                    (String) r[1], // area_type
                    toInteger(r[2]), // credits (points)
                    (String) r[3], // grade
                    (String) r[4], // course_name
                    (Integer) r[5], // semester
                    (Integer) r[6], // year
                    (String) r[7], // course_code
                    toInteger(r[8]), // original_score
                    toInteger(r[9]) // liberalAreaCode (course_offerings.area_code)
                    ))
        .toList();
  }

  /**
   * 학과와 입학 연도에 맞는 단일 전공 졸업 요건을 캐시 우선으로 조회한다.
   *
   * @param deptId 졸업 요건을 조회할 학과 식별자
   * @param admissionYear 졸업 요건 적용 기준 입학 연도
   * @return 캐시 또는 데이터베이스에서 조회한 영역별 졸업 요건
   */
  public List<AreaRequirementDto> getAreaRequirementsWithCache(Long deptId, Integer admissionYear) {
    try {
      List<AreaRequirementDto> cached =
          academicCache.getGraduationRequirements(deptId, admissionYear);
      if (cached != null && !cached.isEmpty()) {
        return cached;
      }

      List<AreaRequirementDto> result = getAreaRequirements(deptId, admissionYear);
      academicCache.setGraduationRequirements(deptId, admissionYear, result);
      return result;

    } catch (Exception e) {
      log.warn(
          "[BIZ] graduation.requirements.cache.fail deptId={} year={} ex={}",
          deptId,
          admissionYear,
          e.getClass().getSimpleName());
      return getAreaRequirements(deptId, admissionYear);
    }
  }

  /**
   * 주전공·복수전공과 입학 연도에 맞는 졸업 요건을 캐시 우선으로 조회한다.
   *
   * @param primaryMajorId 주전공 학과 식별자
   * @param secondaryMajorId 복수전공 학과 식별자
   * @param admissionYear 졸업 요건 적용 기준 입학 연도
   * @return 캐시 또는 데이터베이스에서 조회한 복수전공 영역별 졸업 요건
   */
  public List<AreaRequirementDto> getDualMajorRequirementsWithCache(
      Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
    try {
      List<AreaRequirementDto> cached =
          academicCache.getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
      if (cached != null && !cached.isEmpty()) {
        return cached;
      }

      List<AreaRequirementDto> result =
          getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
      academicCache.setDualMajorRequirements(
          primaryMajorId, secondaryMajorId, admissionYear, result);
      return result;

    } catch (Exception e) {
      log.warn(
          "[BIZ] graduation.dual.requirements.cache.fail primaryId={} secondaryId={} year={} ex={}",
          primaryMajorId,
          secondaryMajorId,
          admissionYear,
          e.getClass().getSimpleName());
      return getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
    }
  }

  /** Number/문자열 숫자 → Integer (null 허용). */
  private static Integer toInteger(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof Number n) {
      return n.intValue();
    }
    if (o instanceof String s) {
      String t = s.trim();
      if (t.isEmpty() || t.equalsIgnoreCase("null")) {
        return null;
      }
      return new java.math.BigDecimal(t).intValue(); // 안전 파싱
    }
    throw new ClassCastException("숫자 아님: " + o);
  }

  private FacultyDivision parseDivision(String raw) {
    if (raw == null) {
      return null;
    }
    return FacultyDivision.valueOf(raw.trim());
  }

  private String normalizeAreaType(String raw) {
    return raw == null ? null : raw.trim();
  }

  private void appendEtcAreaIfPresent(
      List<AreaProgressDto> target, Map<String, List<CourseInternalDto>> coursesByArea) {
    if (target.stream().anyMatch(dto -> dto.getAreaType() == FacultyDivision.기타)) {
      return;
    }

    List<CourseInternalDto> etcCourses =
        coursesByArea.getOrDefault(AREA_ETC, Collections.emptyList());
    if (etcCourses.isEmpty()) {
      return;
    }

    target.add(buildEtcAreaProgress(etcCourses));
  }

  private AreaProgressDto buildEtcAreaProgress(List<CourseInternalDto> etcCourses) {
    int completedElectiveCourses =
        (int) etcCourses.stream().map(CourseInternalDto::getOfferingId).distinct().count();

    int earnedCredits =
        etcCourses.stream()
            .mapToInt(course -> course.getCredits() != null ? course.getCredits() : 0)
            .sum();

    List<CourseDto> courseDtos = etcCourses.stream().map(this::toCourseResponseDto).toList();

    return new AreaProgressDto(
        FacultyDivision.기타,
        ETC_REQUIRED_CREDITS,
        earnedCredits,
        null,
        completedElectiveCourses,
        null,
        courseDtos);
  }

  private Map<String, List<CourseInternalDto>> groupCoursesByArea(List<CourseInternalDto> courses) {
    return courses.stream()
        .collect(
            Collectors.groupingBy(
                dto -> {
                  String area = dto.getAreaType();
                  if (area == null || area.isBlank()) {
                    return AREA_ETC;
                  }
                  return area.trim();
                }));
  }

  private int countCompletedElectiveCourses(String areaType, List<CourseInternalDto> courses) {
    String normalizedAreaType = normalizeAreaType(areaType);
    if (FacultyDivision.선교.name().equals(normalizedAreaType)) {
      return (int)
          courses.stream()
              .map(CourseInternalDto::getLiberalAreaCode)
              .filter(Objects::nonNull)
              .distinct()
              .count();
    }
    return (int) courses.stream().map(CourseInternalDto::getOfferingId).distinct().count();
  }

  /**
   * 내부 교과목 정보를 API 응답으로 변환한다.
   *
   * @param dto API 과목 응답으로 변환할 내부 이수 과목
   * @return 공개 응답 형식으로 변환한 이수 과목
   */
  public CourseDto toCourseResponseDto(CourseInternalDto dto) {
    return new CourseDto(
        dto.getYear(),
        dto.getCourseName(),
        dto.getCredits(),
        dto.getGrade(),
        dto.getSemester(),
        missionLiberalAreaCode(dto));
  }

  private Integer missionLiberalAreaCode(CourseInternalDto dto) {
    if (!FacultyDivision.선교.name().equals(dto.getAreaType())) {
      return null;
    }
    return dto.getLiberalAreaCode();
  }
}
