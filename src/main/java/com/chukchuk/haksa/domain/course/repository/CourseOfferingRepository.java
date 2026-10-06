package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** 과목 개설 정보를 학기·분반·교수·영역 조건으로 조회한다. */
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
  /**
   * 과목·학기·분반·교수·영역·주관 학과가 모두 일치하는 개설 강의를 찾는다.
   *
   * @param courseId 과목 식별자
   * @param year 연도
   * @param semester 개설 학기
   * @param classSection 분반
   * @param professorId 교수 식별자
   * @param facultyDivisionName 과목 영역
   * @param hostDepartment 주관 학과
   * @return 모든 조건이 일치하는 개설 강의가 있으면 포함한 선택값
   */
  @Query(
      """
    SELECT o FROM CourseOffering o
    WHERE o.course.id = :courseId
      AND o.year = :year
      AND o.semester = :semester
      AND o.classSection = :classSection
      AND o.professor.id = :professorId
      AND o.facultyDivisionName = :facultyDivisionName
      AND o.hostDepartment = :hostDepartment
      """)
  Optional<CourseOffering> findMatchingOffering(
      Long courseId,
      Integer year,
      Integer semester,
      String classSection,
      Long professorId,
      FacultyDivision facultyDivisionName,
      String hostDepartment);

  /**
   * 주어진 과목·연도·학기 조합에 포함되는 개설 강의를 조회한다.
   *
   * @param courseIds 조회할 과목 식별자 집합
   * @param years 조회할 연도 집합
   * @param semesters 조회할 학기 집합
   * @return 세 조건 집합에 모두 속하는 개설 강의
   */
  @Query(
      """
    SELECT o FROM CourseOffering o
    WHERE o.course.id IN :courseIds
      AND o.year IN :years
      AND o.semester IN :semesters
      """)
  List<CourseOffering> findByCourseIdInAndYearInAndSemesterIn(
      Collection<Long> courseIds, Collection<Integer> years, Collection<Integer> semesters);

  /**
   * 관리자 테스트에 사용할 개설 강의를 선택 조건으로 검색한다.
   *
   * @param keyword 검색어
   * @param area 과목 영역 필터, {@code null}이면 제한하지 않음
   * @param year 연도
   * @param semester 학기 필터, {@code null}이면 제한하지 않음
   * @param departmentName 학과 이름
   * @return 최신 학기와 과목명 순으로 정렬된 개설 강의 후보
   */
  @Query(
      """
        SELECT o FROM CourseOffering o
        JOIN FETCH o.course c
        LEFT JOIN FETCH o.department d
        LEFT JOIN FETCH o.professor p
        WHERE o.deletedAt IS NULL
          AND (:keyword IS NULL
               OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:area IS NULL OR o.facultyDivisionName = :area)
          AND (:year IS NULL OR o.year = :year)
          AND (:semester IS NULL OR o.semester = :semester)
          AND (:departmentName IS NULL
               OR d.establishedDepartmentName = :departmentName
               OR o.hostDepartment = :departmentName)
        ORDER BY o.year DESC, o.semester DESC, c.courseName ASC
      """)
  List<CourseOffering> searchAdminCandidates(
      String keyword, FacultyDivision area, Integer year, Integer semester, String departmentName);

  /**
   * 강의평가 테스트에 재사용할 특정 학기의 개설 강의를 조회한다.
   *
   * @param year 연도
   * @param semester 조회할 학기
   * @return 과목명·교수명·개설 식별자 순으로 정렬된 개설 강의
   */
  @Query(
      """
        SELECT o FROM CourseOffering o
        JOIN FETCH o.course c
        JOIN FETCH o.professor p
        LEFT JOIN FETCH o.department d
        LEFT JOIN FETCH o.liberalArtsAreaCode lac
        WHERE o.deletedAt IS NULL
          AND o.year = :year
          AND o.semester = :semester
        ORDER BY c.courseName ASC, p.professorName ASC, o.id ASC
      """)
  List<CourseOffering> findReusableLectureEvaluationTestOfferings(Integer year, Integer semester);

  /**
   * 대상 학기의 과목 개설 강의평가 유형을 미확인 상태로 초기화한다.
   *
   * @param year 연도
   * @param semester 초기화할 학기
   * @return 강의평가 유형을 변경한 개설 강의 수
   */
  @Modifying
  @Query(
      value =
          """
        UPDATE course_offerings
        SET evaluation_type_code = 'UNKNOWN'
        WHERE deleted_at IS NULL
          AND year = :year
          AND semester = :semester
          AND (
              evaluation_type_code IS NULL
              OR evaluation_type_code NOT IN ('ABSOLUTE', 'RELATIVE', 'UNKNOWN')
          )
          """,
      nativeQuery = true)
  int normalizeUnsupportedEvaluationTypes(Integer year, Integer semester);
}
