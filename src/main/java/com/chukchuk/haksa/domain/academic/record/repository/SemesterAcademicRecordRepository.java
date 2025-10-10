package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemesterAcademicRecordRepository extends JpaRepository<SemesterAcademicRecord, UUID> {

    @Query("""
    SELECT sar
    FROM SemesterAcademicRecord sar
    WHERE sar.student.id = :studentId
      AND sar.year = :year
      AND sar.semester = :semester
""")
    Optional<SemesterAcademicRecord> findByStudentIdAndYearAndSemester(
            @Param("studentId") UUID studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    List<SemesterAcademicRecord> findByStudentId(UUID studentId); //studentID로 data 얻어오기

    List<SemesterAcademicRecord> findByStudentIdOrderByYearDescSemesterDesc(UUID studentId);
}
