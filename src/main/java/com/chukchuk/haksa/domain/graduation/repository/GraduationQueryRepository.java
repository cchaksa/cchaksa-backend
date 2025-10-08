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

    /* 졸업 요건 조회 (학과 코드, ) */
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

    // 이수구분 별 졸업 요건 결과 캐싱 로직
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