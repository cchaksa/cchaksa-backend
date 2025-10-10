package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {
    @Query("""
        SELECT sc FROM StudentCourse sc
        JOIN FETCH sc.offering co
        JOIN FETCH co.course c
        LEFT JOIN FETCH co.professor p
        WHERE sc.student.id = :studentId
        AND co.year = :year
        AND co.semester = :semester
    """)
    List<StudentCourse> findByStudentIdAndYearAndSemester(
            @Param("studentId") UUID studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    List<StudentCourse> findByStudent(Student student);
}