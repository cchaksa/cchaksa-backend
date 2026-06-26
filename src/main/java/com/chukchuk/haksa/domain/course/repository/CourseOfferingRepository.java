package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    @Query("""
    SELECT o FROM CourseOffering o
    WHERE o.course.id = :courseId
      AND o.year = :year
      AND o.semester = :semester
      AND o.classSection = :classSection
      AND o.professor.id = :professorId
      AND o.facultyDivisionName = :facultyDivisionName
      AND o.hostDepartment = :hostDepartment
""")
    Optional<CourseOffering> findByCourseIdAndYearAndSemesterAndClassSectionAndProfessorIdAndFacultyDivisionNameAndHostDepartment(
            Long courseId, Integer year, Integer semester, String classSection, Long professorId, FacultyDivision facultyDivisionName, String hostDepartment
    );

    @Query("""
    SELECT o FROM CourseOffering o
    WHERE o.course.id IN :courseIds
      AND o.year IN :years
      AND o.semester IN :semesters
""")
    List<CourseOffering> findByCourseIdInAndYearInAndSemesterIn(
            Collection<Long> courseIds,
            Collection<Integer> years,
            Collection<Integer> semesters
    );

    @Query("""
        SELECT o FROM CourseOffering o
        JOIN FETCH o.course c
        LEFT JOIN FETCH o.department d
        LEFT JOIN FETCH o.professor p
        WHERE o.deletedAt IS NULL
          AND (:keyword IS NULL
               OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:area IS NULL OR o.facultyDivisionName = :area)
          AND (:year IS NULL OR o.year = :year)
          AND (:semester IS NULL OR o.semester = :semester)
          AND (:departmentName IS NULL
               OR d.establishedDepartmentName = :departmentName
               OR o.hostDepartment = :departmentName)
        ORDER BY o.year DESC, o.semester DESC, c.courseName ASC
    """)
    List<CourseOffering> searchAdminCandidates(
            String keyword,
            FacultyDivision area,
            Integer year,
            Integer semester,
            String departmentName
    );

    @Query("""
        SELECT o FROM CourseOffering o
        JOIN FETCH o.course c
        JOIN FETCH o.professor p
        LEFT JOIN FETCH o.department d
        LEFT JOIN FETCH o.liberalArtsAreaCode lac
        WHERE o.deletedAt IS NULL
          AND o.year = :year
          AND o.semester = :semester
          AND o.course IS NOT NULL
          AND o.professor IS NOT NULL
        ORDER BY c.courseName ASC, p.professorName ASC, o.id ASC
    """)
    List<CourseOffering> findReusableLectureEvaluationTestOfferings(Integer year, Integer semester);
}
