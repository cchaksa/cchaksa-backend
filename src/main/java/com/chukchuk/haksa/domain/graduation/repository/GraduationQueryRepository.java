package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.AreaRequirementDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseInternalDto;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GraduationQueryRepository {
    private final EntityManager em;
    private final ObjectMapper ob;
    private final RedisCacheStore redisCacheStore;

    /* 졸업 요건 조회 (학과 코드, 입학년도) */
    public List<AreaRequirementDto> getAreaRequirements(Long departmentId, Integer admissionYear) {
        String sql = """
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
                .map(row -> new AreaRequirementDto(
                        (String) row[0],              // area_type
                        toInteger(row[1]),            // required_credits
                        toInteger(row[2]),            // required_elective_courses (nullable)
                        toInteger(row[3])             // total_elective_courses (nullable)
                ))
                .toList();
    }

    /* 복수 전공 졸업 요건 조회 (학과 코드, 입학년도) */
    private List<AreaRequirementDto> getDualMajorRequirements(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
        String sql = """
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
                .map(row -> new AreaRequirementDto(
                        (String) row[0],
                        toInteger(row[1]),
                        toInteger(row[2]),
                        toInteger(row[3])
                ))
                .toList();
    }

    public List<AreaProgressDto> getStudentAreaProgress(UUID studentId, Long departmentId, Integer admissionYear) {
        long t0 = LogTime.start();

        List<AreaRequirementDto> areaRequirements = getAreaRequirementsWithCache(departmentId, admissionYear);
        List<CourseInternalDto> completedCourses = getLatestValidCourses(studentId);

        Map<String, List<CourseInternalDto>> coursesByArea = completedCourses.stream()
                .collect(Collectors.groupingBy(CourseInternalDto::getAreaType));

        List<AreaProgressDto> result = new ArrayList<>();

        for (AreaRequirementDto req : areaRequirements) {
            List<CourseInternalDto> taken = coursesByArea.getOrDefault(req.areaType(), Collections.emptyList());

            int earnedCredits = taken.stream().mapToInt(CourseInternalDto::getCredits).sum();
            int completedElectiveCourses = (int) taken.stream()
                    .map(CourseInternalDto::getOfferingId)
                    .distinct()
                    .count();

            List<CourseDto> courseDtos = taken.stream()
                    .map(this::toCourseResponseDto)
                    .toList();

            AreaProgressDto dto = new AreaProgressDto(
                    parseDivision(req.areaType()),
                    req.requiredCredits(),
                    earnedCredits,
                    req.requiredElectiveCourses(),
                    completedElectiveCourses,
                    req.totalElectiveCourses(),
                    courseDtos
            );
            result.add(dto);
        }

        long tookMS = LogTime.elapsedMs(t0);
        if (tookMS >= SLOW_MS) {
            log.info("[BIZ] graduation.progress.query.done studentId={} deptId={} admissionYear={} rows={} took_ms={}",
                    studentId, departmentId, admissionYear, result.size(), tookMS);
        }

        return result;
    }

    /**
     * 주전공의 전공 기초 교양과 과목이 겹치는 경우 테스트 필요
     * 주전공 기존 전선 졸업 요건 -> 복수전공용 전선1로 대체
     * 복수전공 졸업 요건 영역: 전교, 전필, 전선
     */
    public List<AreaProgressDto> getDualMajorAreaProgress(UUID studentId, Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
        long t0 = LogTime.start();

        // 주전공 졸업 요건 조회
        List<AreaRequirementDto> primaryReqs = getAreaRequirementsWithCache(primaryMajorId, admissionYear);

        // 주전공 졸업 요건 중 '전선' 제외
        List<AreaRequirementDto> primaryFiltered = primaryReqs.stream()
                .filter(req -> !req.areaType().equalsIgnoreCase("전선"))
                .toList();

        // 복수전공 및 주전공 전선1 졸업 요건 조회
        List<AreaRequirementDto> dualMajorReqs = getDualMajorRequirementsWithCache(primaryMajorId, secondaryMajorId, admissionYear);

        // 전체 병합
        List<AreaRequirementDto> mergedRequirements = new ArrayList<>();
        mergedRequirements.addAll(primaryFiltered);   // 주전공 졸업 요건 (전선 제외)
        mergedRequirements.addAll(dualMajorReqs);     // 복수전공용 졸업 요건

        // 수강 이력 조회
        List<CourseInternalDto> completedCourses = getLatestValidCourses(studentId);
        Map<String, List<CourseInternalDto>> coursesByArea = completedCourses.stream()
                .collect(Collectors.groupingBy(CourseInternalDto::getAreaType));

        // 이수 현황 계산
        List<AreaProgressDto> result = new ArrayList<>();
        for (AreaRequirementDto req : mergedRequirements) {
            List<CourseInternalDto> taken = coursesByArea.getOrDefault(req.areaType(), Collections.emptyList());

            int earnedCredits = taken.stream().mapToInt(CourseInternalDto::getCredits).sum();
            int completedElectiveCourses = (int) taken.stream()
                    .map(CourseInternalDto::getOfferingId)
                    .distinct()
                    .count();

            List<CourseDto> courseDtos = taken.stream()
                    .map(this::toCourseResponseDto)
                    .toList();

            AreaProgressDto dto = new AreaProgressDto(
                    parseDivision(req.areaType()),
                    req.requiredCredits(),
                    earnedCredits,
                    req.requiredElectiveCourses(),
                    completedElectiveCourses,
                    req.totalElectiveCourses(),
                    courseDtos
            );

            // 전공 구분 설정 -> 논의 후 적용
//            if (req.areaType().equalsIgnoreCase("전선1")) dto.setMajorType("MAJOR1");
//            else if (req.areaType().equalsIgnoreCase("전선2")
//                    || req.areaType().equalsIgnoreCase("전필")
//                    || req.areaType().equalsIgnoreCase("전교")) dto.setMajorType("MAJOR2");
//            else dto.setMajorType("MAJOR1");

            result.add(dto);
        }

        long tookMS = LogTime.elapsedMs(t0);
        if (tookMS >= SLOW_MS) {
            log.info("[BIZ] graduation.dual.progress.query.done studentId={} primaryDept={} secondaryDept={} year={} rows={} took_ms={}",
                    studentId, primaryMajorId, secondaryMajorId, admissionYear, result.size(), tookMS);
        }

        return result;
    }

    public List<CourseInternalDto> getLatestValidCourses(UUID studentId) {
        String sql = """
            SELECT DISTINCT ON (c.course_code, co.faculty_division_name)
                sc.offering_id,
                TRIM(co.faculty_division_name) AS area_type,
                co.points,
                sc.grade,
                c.course_name,
                co.semester,
                co.year,
                c.course_code,
                sc.original_score
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
                .map(r -> new CourseInternalDto(
                        (Long) r[0],               // offering_id
                        (String) r[1],             // area_type
                        toInteger(r[2]),           // credits (points)
                        (String) r[3],             // grade
                        (String) r[4],             // course_name
                        (Integer) r[5],            // semester
                        (Integer) r[6],            // year
                        (String) r[7],             // course_code
                        toInteger(r[8])            // original_score
                ))
                .toList();
    }

    /**
     * 단일 전공 이수구분 별 졸업 요건 결과 캐싱 로직
     * 학과 ID + 입학년도
     */
    public List<AreaRequirementDto> getAreaRequirementsWithCache(Long deptId, Integer admissionYear) {
        try {
            List<AreaRequirementDto> cached = redisCacheStore.getGraduationRequirements(deptId, admissionYear);
            if (cached != null && !cached.isEmpty()) return cached;

            List<AreaRequirementDto> result = getAreaRequirements(deptId, admissionYear);
            redisCacheStore.setGraduationRequirements(deptId, admissionYear, result);
            return result;

        } catch (Exception e) {
            log.warn("[BIZ] graduation.requirements.cache.fail deptId={} year={} ex={}", deptId, admissionYear, e.getClass().getSimpleName());
            return getAreaRequirements(deptId, admissionYear);
        }
    }

    /**
     * 복수 전공 이수구분 별 졸업 요건 결과 캐싱 로직
     * 주전공 ID + 복수전공 ID + 입학년도
     */
    public List<AreaRequirementDto> getDualMajorRequirementsWithCache(Long primaryMajorId, Long secondaryMajorId, Integer admissionYear) {
        try {
            List<AreaRequirementDto> cached = redisCacheStore.getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
            if (cached != null && !cached.isEmpty()) return cached;

            List<AreaRequirementDto> result = getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
            redisCacheStore.setDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear, result);
            return result;

        } catch (Exception e) {
            log.warn("[BIZ] graduation.dual.requirements.cache.fail primaryId={} secondaryId={} year={} ex={}",
                    primaryMajorId, secondaryMajorId, admissionYear, e.getClass().getSimpleName());
            return getDualMajorRequirements(primaryMajorId, secondaryMajorId, admissionYear);
        }
    }

    /** Number/문자열 숫자 → Integer (null 허용) */
    private static Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            String t = s.trim();
            if (t.isEmpty() || t.equalsIgnoreCase("null")) return null;
            return new java.math.BigDecimal(t).intValue(); // 안전 파싱
        }
        throw new ClassCastException("숫자 아님: " + o);
    }

    private FacultyDivision parseDivision(String raw) {
        if (raw == null) return null;
        return FacultyDivision.valueOf(raw.trim());
    }

    public CourseDto toCourseResponseDto(CourseInternalDto dto) {
        return new CourseDto(
                dto.getYear(),
                dto.getCourseName(),
                dto.getCredits(),
                dto.getGrade(),
                dto.getSemester()
        );
    }
}