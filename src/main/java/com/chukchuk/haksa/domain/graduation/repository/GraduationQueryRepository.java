package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.graduation.dto.AreaProgressDto;
import com.chukchuk.haksa.domain.graduation.dto.CourseDto;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.LogTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.chukchuk.haksa.global.logging.LoggingThresholds.SLOW_MS;

@Repository
@RequiredArgsConstructor
@Slf4j
public class GraduationQueryRepository {
    private final EntityManager em;
    private final ObjectMapper ob;

    /* 필요 졸업 학점 계산 로직 */
    public Integer getTotalRequiredGraduationCredits(Long departmentId, Integer admissionYear) {
        String sql = """
        SELECT COALESCE(SUM(dar.required_credits), 0)
        FROM department_area_requirements dar
        WHERE dar.department_id = :departmentId
          AND dar.admission_year = :admissionYear
    """;

        Query query = em.createNativeQuery(sql);
        query.setParameter("departmentId", departmentId);
        query.setParameter("admissionYear", admissionYear);

        Object result = query.getSingleResult();
        return ((Number) result).intValue();
    }

    public List<AreaProgressDto> getStudentAreaProgress(UUID studentId, Long departmentId, Integer admissionYear) {
        long t0 = LogTime.start();

        String sql = """
WITH raw_courses AS (
    SELECT DISTINCT ON (c.course_code, co.faculty_division_name)
        sc.offering_id,
        co.faculty_division_name AS raw_area_type,
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
    ORDER BY 
        c.course_code, 
        co.faculty_division_name,
        co.year DESC,                       
        co.semester DESC,
        sc.original_score DESC
),
latest_courses AS (
    SELECT 
        offering_id,
        TRIM(raw_area_type) AS area_type,
        points,
        grade,
        course_name,
        semester,
        year,
        course_code,
        original_score
    FROM raw_courses
),
area_requirements AS (
    SELECT 
        dar.area_type,
        dar.required_credits,
        dar.required_elective_courses,
        dar.total_elective_courses
    FROM department_area_requirements dar
    WHERE dar.department_id = :effectiveDepartmentId
      AND dar.admission_year = :admissionYear
),
aggregated_progress AS (
    SELECT 
        ar.area_type AS areaType,
        ar.required_credits AS requiredCredits,
        CAST(COALESCE(SUM(lc.points), 0) AS INTEGER) AS earnedCredits,
        ar.required_elective_courses AS requiredElectiveCourses,
        CAST(COUNT(DISTINCT lc.offering_id) AS INTEGER) AS completedElectiveCourses,
        ar.total_elective_courses AS totalElectiveCourses
    FROM area_requirements ar
    LEFT JOIN latest_courses lc ON lc.area_type = ar.area_type
    GROUP BY 
        ar.area_type,
        ar.required_credits,
        ar.required_elective_courses,
        ar.total_elective_courses
)
SELECT 
    ap.*,
    CASE 
        WHEN COUNT(lc.offering_id) = 0 THEN NULL 
        ELSE CAST(json_agg(
            json_build_object(
                'courseName', lc.course_name,
                'credits', lc.points,
                'grade', lc.grade,
                'semester', lc.semester,
                'year', lc.year,
                'originalScore', lc.original_score
            )
        ) AS TEXT)
    END AS courses
FROM aggregated_progress ap
LEFT JOIN latest_courses lc ON lc.area_type = ap.areaType
GROUP BY ap.areaType, ap.requiredCredits, ap.earnedCredits, 
         ap.requiredElectiveCourses, ap.completedElectiveCourses, ap.totalElectiveCourses;
""";

        Query query = em.createNativeQuery(sql);
        query.setParameter("studentId", studentId);
        query.setParameter("effectiveDepartmentId", departmentId);
        query.setParameter("admissionYear", admissionYear);

        // 기존 info: results 전체 출력 → 제거 (PII/용량 리스크)
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            throw new CommonException(ErrorCode.GRADUATION_REQUIREMENTS_NOT_FOUND);
        }

        List<AreaProgressDto> list = results.stream().map(this::mapToDto).collect(Collectors.toList());

        long tookMS = LogTime.elapsedMs(t0);
        if (tookMS >= SLOW_MS) {
            // PII 없음: dept/admission/row수만
            log.info("[BIZ] graduation.progress.query.done deptId={} admissionYear={} rows={} took_ms={}",
                    departmentId, admissionYear, list.size(), tookMS);
        }

        return list;
    }

    private AreaProgressDto mapToDto(Object[] row) {
        try {
            FacultyDivision areaType = parseDivision(row[0]);
            Integer requiredCredits = toInteger(row[1]);
            Integer earnedCredits = toInteger(row[2]);
            Integer requiredElectiveCourses = toInteger(row[3]);
            Integer completedElectiveCourses = toInteger(row[4]);
            Integer totalElectiveCourses = toInteger(row[5]);
            List<CourseDto> courses = parseCourses(row[6]);

            return new AreaProgressDto(
                    areaType, requiredCredits, earnedCredits,
                    requiredElectiveCourses, completedElectiveCourses, totalElectiveCourses, courses
            );

        } catch (IllegalArgumentException | ClassCastException e) {
            log.error("[BIZ] graduation.progress.map.error ex={}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("졸업요건 매핑 오류", e);
        } catch (JsonProcessingException e) {
            log.error("[BIZ] graduation.progress.json.error ex={}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("JSON 변환 오류", e);
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

    /** Enum 파싱: null-safe + trim */
    private static FacultyDivision parseDivision(Object o) {
        if (o == null) return null;
        String v = o.toString().trim();
        return FacultyDivision.valueOf(v);
    }

    /** JSON 배열 텍스트 → List<CourseDto> (null/빈값 방어) */
    private List<CourseDto> parseCourses(Object o) throws JsonProcessingException {
        if (o == null) return java.util.Collections.emptyList();
        String json = o.toString().trim();
        if (json.isEmpty() || "null".equalsIgnoreCase(json)) return java.util.Collections.emptyList();
        return ob.readValue(json, new TypeReference<List<CourseDto>>() {});
    }
}