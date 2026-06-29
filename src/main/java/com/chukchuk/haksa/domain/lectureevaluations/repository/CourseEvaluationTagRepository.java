package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseEvaluationTagRepository extends JpaRepository<CourseEvaluationTag, Long> {

    @Modifying
    @Query("""
        DELETE FROM CourseEvaluationTag tag
        WHERE tag.courseEvaluation.id IN (
            SELECT ce.id
            FROM CourseEvaluation ce
            WHERE ce.student.id = :studentId
              AND ce.year = :year
              AND ce.semester = :semester
        )
    """)
    void deleteByStudentIdAndYearAndSemester(
            @Param("studentId") UUID studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );
}
