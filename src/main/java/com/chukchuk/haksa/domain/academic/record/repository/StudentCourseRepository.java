package com.chukchuk.haksa.domain.academic.record.repository;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.student.model.Student;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 학생의 수강 내역을 학생·연도·학기로 조회하고 소유권 범위에서 삭제하는 저장소다. */
@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {
  /**
   * 학생의 전체 수강 기록과 과목 정보를 한 번의 fetch join으로 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 과목과 개설 정보가 초기화된 수강 기록 목록
   */
  @Query(
      """
        SELECT sc FROM StudentCourse sc
        JOIN FETCH sc.offering co
        JOIN FETCH co.course c
        WHERE sc.student.id = :studentId
      """)
  List<StudentCourse> findAllWithCourseByStudentId(@Param("studentId") UUID studentId);

  /**
   * 학생의 지정 학기 수강 내역을 과목·교수 정보와 함께 조회한다.
   *
   * @param studentId 학생 식별자
   * @param year 대상 연도
   * @param semester 대상 학기
   * @return 지정 학기의 수강 내역 목록
   */
  @Query(
      """
        SELECT sc FROM StudentCourse sc
        JOIN FETCH sc.offering co
        JOIN FETCH co.course c
        LEFT JOIN FETCH co.professor p
        LEFT JOIN FETCH co.liberalArtsAreaCode lac
        WHERE sc.student.id = :studentId
        AND co.year = :year
        AND co.semester = :semester
      """)
  List<StudentCourse> findByStudentIdAndYearAndSemester(
      @Param("studentId") UUID studentId,
      @Param("year") Integer year,
      @Param("semester") Integer semester);

  /**
   * 학생의 전체 수강 내역을 조회한다.
   *
   * @param student 대상 학생
   * @return 학생의 전체 수강 내역 목록
   */
  List<StudentCourse> findByStudent(Student student);

  /**
   * 학생의 전체 수강 내역을 삭제한다.
   *
   * @param studentId 학생 식별자
   */
  void deleteByStudentId(UUID studentId);

  /**
   * 학생이 소유한 지정 식별자의 수강 내역을 삭제한다.
   *
   * @param studentId 학생 식별자
   * @param ids 삭제할 식별자 집합
   */
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM StudentCourse sc WHERE sc.student.id = :studentId AND sc.id IN :ids")
  void deleteOwnedByStudentIdAndIdIn(
      @Param("studentId") UUID studentId, @Param("ids") List<Long> ids);

  /**
   * 학생의 지정 학기 수강 내역을 삭제한다.
   *
   * @param studentId 학생 식별자
   * @param year 대상 연도
   * @param semester 대상 학기
   */
  @Modifying
  @Query(
      """
        DELETE FROM StudentCourse sc
        WHERE sc.student.id = :studentId
          AND sc.offering.year = :year
          AND sc.offering.semester = :semester
      """)
  void deleteByStudentIdAndYearAndSemester(
      @Param("studentId") UUID studentId,
      @Param("year") Integer year,
      @Param("semester") Integer semester);
}
