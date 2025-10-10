package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentAcademicRecordRepository extends JpaRepository<StudentAcademicRecord, UUID> {

    // student_id(UUID)로 직접 조회
    @Query("""
        SELECT r FROM StudentAcademicRecord r
        JOIN FETCH r.student s
        WHERE s.id = :studentId
    """)
    Optional<StudentAcademicRecord> findByStudentId(@Param("studentId") UUID studentId);

    void deleteByStudentId(UUID studentId);
}
