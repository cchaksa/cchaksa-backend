package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {

    @Modifying
    @Query("""
        DELETE FROM CourseEvaluation ce
        WHERE ce.student.id = :studentId
          AND ce.year = :year
          AND ce.semester = :semester
    """)
    void deleteByStudentIdAndYearAndSemester(
            @Param("studentId") UUID studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );
}
