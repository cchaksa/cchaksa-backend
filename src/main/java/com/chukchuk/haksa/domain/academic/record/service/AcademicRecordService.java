package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
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
        Map<CourseCategory, List<StudentCourseDto.CourseDetailDto>> categorizedCourses = categorizeCourses(
                studentCourseService.getStudentCourses(studentId, year, semester));

        List<StudentCourseDto.CourseDetailDto> majorCourses = categorizedCourses.getOrDefault(CourseCategory.MAJOR, List.of());
        List<StudentCourseDto.CourseDetailDto> liberalCourses = categorizedCourses.getOrDefault(CourseCategory.LIBERAL, List.of());
        List<StudentCourseDto.CourseDetailDto> etcCourses = categorizedCourses.getOrDefault(CourseCategory.ETC, List.of());

        return new AcademicRecordResponse(
                semesterGrade,
                new AcademicRecordResponse.Courses(majorCourses, liberalCourses, etcCourses)
        );
    }

    /* Using Method */

    /*과목을 전공/교양으로 분류*/
    private Map<CourseCategory, List<StudentCourseDto.CourseDetailDto>> categorizeCourses(List<StudentCourseDto.CourseDetailDto> courses) {
        return courses.stream()
                .collect(Collectors.groupingBy(this::determineCategory));
    }

    private CourseCategory determineCategory(StudentCourseDto.CourseDetailDto course) {
        FacultyDivision division = course.areaType();
        if (division == null || division == FacultyDivision.기타) {
            return CourseCategory.ETC;
        }

        return switch (division) {
            case 전핵, 전선, 복선 -> CourseCategory.MAJOR;
            default -> CourseCategory.LIBERAL;
        };
    }

    private enum CourseCategory {
        MAJOR,
        LIBERAL,
        ETC
    }
}
