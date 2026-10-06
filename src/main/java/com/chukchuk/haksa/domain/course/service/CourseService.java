package com.chukchuk.haksa.domain.course.service;

import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 과목 코드가 같은 과목을 재사용하거나 새 과목을 저장한다. */
@Service
@RequiredArgsConstructor
public class CourseService {
  private final CourseRepository courseRepository;

  /**
   * 학수번호가 같은 과목을 재사용하고, 없으면 이름과 함께 새로 저장한다.
   *
   * @param courseCode 과목 코드
   * @param courseName 과목 이름
   * @return 학수번호가 같으면 기존 과목, 없으면 새로 저장한 과목
   */
  @Transactional
  public Course getOrCreateCourse(String courseCode, String courseName) {
    return courseRepository
        .findByCourseCode(courseCode)
        .orElseGet(
            () -> {
              Course newCourse = new Course(courseCode, courseName);
              return courseRepository.save(newCourse);
            });
  }

  /**
   * 학수번호별 과목을 일괄 재사용하거나 저장한다.
   *
   * @param courseCodeToName 과목 code to 이름
   * @return 학수번호와 재사용하거나 새로 저장한 과목의 대응표
   */
  @Transactional
  public Map<String, Course> getOrCreateCourses(Map<String, String> courseCodeToName) {
    if (courseCodeToName == null || courseCodeToName.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> codes =
        courseCodeToName.keySet().stream()
            .filter(code -> code != null && !code.isBlank())
            .collect(Collectors.toSet());
    if (codes.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Course> existing = courseRepository.findByCourseCodeIn(codes);
    Map<String, Course> result = new HashMap<>();
    for (Course course : existing) {
      result.put(course.getCourseCode(), course);
    }

    List<Course> toCreate =
        codes.stream()
            .filter(code -> !result.containsKey(code))
            .map(code -> new Course(code, courseCodeToName.get(code)))
            .toList();

    if (!toCreate.isEmpty()) {
      List<Course> saved = courseRepository.saveAll(toCreate);
      for (Course course : saved) {
        result.put(course.getCourseCode(), course);
      }
    }

    return result;
  }
}
