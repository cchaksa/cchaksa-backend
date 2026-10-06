package com.chukchuk.haksa.domain.course.repository;

import com.chukchuk.haksa.domain.course.model.Course;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 과목 코드와 이름으로 과목을 조회하는 저장소다. */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
  /**
   * 과목 코드가 일치하는 과목을 조회한다.
   *
   * @param courseCode 과목 코드
   * @return 과목 코드가 일치하면 포함한 선택값
   */
  Optional<Course> findByCourseCode(String courseCode);

  /**
   * 주어진 과목 코드 중 하나와 일치하는 과목을 조회한다.
   *
   * @param courseCodes 조회할 과목 코드 모음
   * @return 주어진 코드와 일치하는 과목 목록
   */
  List<Course> findByCourseCodeIn(Collection<String> courseCodes);
}
