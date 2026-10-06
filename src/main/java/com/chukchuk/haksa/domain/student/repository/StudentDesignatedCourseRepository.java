// 학생별 지정과목 원본을 조회하고 일괄 삭제한다.

package com.chukchuk.haksa.domain.student.repository;

import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생 지정과목을 원본 배열 순서로 조회하고 학생 단위로 삭제한다. */
@Repository
public interface StudentDesignatedCourseRepository
    extends JpaRepository<StudentDesignatedCourse, Long> {

  /**
   * 학생의 지정과목을 원본 배열 순서로 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 원본 순서로 정렬된 지정과목 목록
   */
  List<StudentDesignatedCourse> findAllByStudentIdOrderBySourceOrder(UUID studentId);

  /**
   * 학생의 지정과목을 일괄 삭제한다.
   *
   * @param studentId 학생 식별자
   * @return 삭제된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM StudentDesignatedCourse c WHERE c.student.id = :studentId")
  int deleteAllByStudentId(@Param("studentId") UUID studentId);
}
