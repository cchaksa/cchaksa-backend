package com.chukchuk.haksa.domain.lectureevaluations.repository;

import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluationTag;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생의 학기별 강의평가에 연결된 선택 태그를 일괄 삭제하는 저장소다. */
@Repository
public interface CourseEvaluationTagRepository extends JpaRepository<CourseEvaluationTag, Long> {

  /**
   * 학생의 특정 학기 강의평가에 연결된 모든 태그를 삭제한다.
   *
   * @param studentId 학생 식별자
   * @param year 대상 연도
   * @param semester 대상 학기
   */
  @Modifying
  @Query(
      """
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
      @Param("semester") Integer semester);
}
