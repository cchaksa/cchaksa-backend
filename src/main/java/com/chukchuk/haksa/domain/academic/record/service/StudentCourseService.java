package com.chukchuk.haksa.domain.academic.record.service;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto.CourseDetailDto;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생이 학기별로 수강한 과목과 성적을 조회한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCourseService {
  private final StudentCourseRepository studentCourseRepository;

  /**
   * 학생이 특정 학기에 수강한 과목을 성적과 함께 반환한다.
   *
   * @param studentId 학생 식별자
   * @param year 연도
   * @param semester 조회할 학기
   * @return 해당 학기의 수강 과목 상세 목록
   */
  public List<CourseDetailDto> getStudentCourses(UUID studentId, Integer year, Integer semester) {
    List<StudentCourse> courses =
        studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, year, semester);

    return courses.stream().map(CourseDetailDto::from).collect(Collectors.toList());
  }
}
