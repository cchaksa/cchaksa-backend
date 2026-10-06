package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생의 학기 성적과 수강 과목을 영역별로 구성해 제공한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicRecordService {
  private final SemesterAcademicRecordService semesterAcademicRecordService;
  private final StudentCourseService studentCourseService;

  /* 학기별 성적 및 수강 과목 정보 조회 */
  /**
   * 학생의 특정 학기 성적과 수강 과목을 전공·교양·기타 영역으로 분류해 반환한다.
   *
   * @param studentId 학생 식별자
   * @param year 연도
   * @param semester 조회할 학기
   * @return 학기 성적과 영역별 수강 과목
   */
  public AcademicRecordResponse getAcademicRecord(UUID studentId, Integer year, Integer semester) {

    // 학기별 성적 조회
    SemesterAcademicRecordDto.SemesterGradeResponse semesterGrade =
        semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, year, semester);

    // 수강 과목 조회 및 카테고리 분류
    Map<CourseCategory, List<StudentCourseDto.CourseDetailDto>> categorizedCourses =
        categorizeCourses(studentCourseService.getStudentCourses(studentId, year, semester));

    List<StudentCourseDto.CourseDetailDto> majorCourses =
        categorizedCourses.getOrDefault(CourseCategory.MAJOR, List.of());
    List<StudentCourseDto.CourseDetailDto> liberalCourses =
        categorizedCourses.getOrDefault(CourseCategory.LIBERAL, List.of());
    List<StudentCourseDto.CourseDetailDto> etcCourses =
        categorizedCourses.getOrDefault(CourseCategory.ETC, List.of());

    return new AcademicRecordResponse(
        semesterGrade,
        new AcademicRecordResponse.Courses(majorCourses, liberalCourses, etcCourses));
  }

  /* Using Method */

  /*과목을 전공/교양으로 분류*/
  private Map<CourseCategory, List<StudentCourseDto.CourseDetailDto>> categorizeCourses(
      List<StudentCourseDto.CourseDetailDto> courses) {
    return courses.stream().collect(Collectors.groupingBy(this::determineCategory));
  }

  private CourseCategory determineCategory(StudentCourseDto.CourseDetailDto course) {
    FacultyDivision division = course.areaType();
    if (division == null || division == FacultyDivision.기타) {
      return CourseCategory.ETC;
    }

    return switch (division) {
      case 전핵, 전선, 복핵, 복선 -> CourseCategory.MAJOR;
      default -> CourseCategory.LIBERAL;
    };
  }

  private enum CourseCategory {
    MAJOR,
    LIBERAL,
    ETC
  }
}
