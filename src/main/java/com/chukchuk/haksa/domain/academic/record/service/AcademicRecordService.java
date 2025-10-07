package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicRecordService {
    private final SemesterAcademicRecordService semesterAcademicRecordService;
    private final StudentCourseService studentCourseService;


    /* 학기별 성적 및 수강 과목 정보 조회 */
    public AcademicRecordResponse getAcademicRecord(UUID studentId, Integer year, Integer semester) {

        // 학기별 성적 조회
        SemesterAcademicRecordDto.SemesterGradeResponse semesterGrade =
                semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, year, semester);

        // 수강 과목 조회 및 카테고리 분류
        Map<String, List<StudentCourseDto.CourseDetailDto>> categorizedCourses = categorizeCourses(
                studentCourseService.getStudentCourses(studentId, year, semester));

        List<StudentCourseDto.CourseDetailDto> majorCourses = categorizedCourses.getOrDefault("major", List.of());
        List<StudentCourseDto.CourseDetailDto> liberalCourses = categorizedCourses.getOrDefault("liberal", List.of());

        return new AcademicRecordResponse(
                semesterGrade,
                new AcademicRecordResponse.Courses(majorCourses, liberalCourses)
        );
    }

    /* Using Method */

    /*과목을 전공/교양으로 분류*/
    private Map<String, List<StudentCourseDto.CourseDetailDto>> categorizeCourses(List<StudentCourseDto.CourseDetailDto> courses) {
        return courses.stream()
                .collect(Collectors.groupingBy(course -> switch (course.areaType()) {
                    case 전핵, 전선, 복선 -> "major";
                    default -> "liberal";
                }));
    }
}
