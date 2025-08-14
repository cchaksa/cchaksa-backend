package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
""")
    Optional<CourseOffering> findByCourseIdAndYearAndSemesterAndClassSectionAndProfessorIdAndFacultyDivisionName(
            Long courseId, Integer year, Integer semester, String classSection, Long professorId, FacultyDivision facultyDivisionName
    );
}
