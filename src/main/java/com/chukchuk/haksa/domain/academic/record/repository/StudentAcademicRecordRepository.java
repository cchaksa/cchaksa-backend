package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생의 누적 성적을 학생 식별자와 함께 조회하고 학생 단위로 삭제하는 저장소다. */
@Repository
public interface StudentAcademicRecordRepository
    extends JpaRepository<StudentAcademicRecord, UUID> {

  // student_id(UUID)로 직접 조회
  /**
   * 학생의 누적 성적을 학생 정보와 함께 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 누적 성적이 있으면 포함한 선택값
   */
  @Query(
      """
        SELECT r FROM StudentAcademicRecord r
        JOIN FETCH r.student s
        WHERE s.id = :studentId
      """)
  Optional<StudentAcademicRecord> findByStudentId(@Param("studentId") UUID studentId);

  /**
   * 학생의 누적 성적을 삭제한다.
   *
   * @param studentId 학생 식별자
   */
  void deleteByStudentId(UUID studentId);
}
