package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생의 학기별 강의평가 결과를 저장하고 일괄 삭제하는 저장소다. */
@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {

  /**
   * 학생의 지정 학기 강의평가를 모두 삭제한다.
   *
   * @param studentId 학생 식별자
   * @param year 대상 연도
   * @param semester 대상 학기
   */
  @Modifying
  @Query(
      """
        DELETE FROM CourseEvaluation ce
        WHERE ce.student.id = :studentId
          AND ce.year = :year
          AND ce.semester = :semester
      """)
  void deleteByStudentIdAndYearAndSemester(
      @Param("studentId") UUID studentId,
      @Param("year") Integer year,
      @Param("semester") Integer semester);
}
