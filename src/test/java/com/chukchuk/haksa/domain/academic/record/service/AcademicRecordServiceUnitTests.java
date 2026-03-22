package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto;
import com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicRecordServiceUnitTests {

    @Mock
    private SemesterAcademicRecordService semesterAcademicRecordService;

    @Mock
    private StudentCourseService studentCourseService;

    @InjectMocks
    private AcademicRecordService academicRecordService;

    @Test
    @DisplayName("전공/교양 과목을 분류해 학업 기록 응답을 반환한다")
    void getAcademicRecord_categorizesCourses() {
        UUID studentId = UUID.randomUUID();
        SemesterAcademicRecordDto.SemesterGradeResponse grade =
                new SemesterAcademicRecordDto.SemesterGradeResponse(
                        2024, 1, 15, 18, new BigDecimal("3.80"), 5, 120, new BigDecimal("92.4")
                );

        StudentCourseDto.CourseDetailDto major = new StudentCourseDto.CourseDetailDto(
                "1", "자료구조", "CSE101", FacultyDivision.전핵, 3, "홍길동", "A+", 3,
                false, false, 2024, 1, 95, false
        );
        StudentCourseDto.CourseDetailDto liberal = new StudentCourseDto.CourseDetailDto(
                "2", "글쓰기", "LBA101", FacultyDivision.중핵, 2, "김교수", "B+", 2,
                false, false, 2024, 1, 88, false
        );

        when(semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, 2024, 1)).thenReturn(grade);
        when(studentCourseService.getStudentCourses(studentId, 2024, 1)).thenReturn(List.of(major, liberal));

        AcademicRecordResponse result = academicRecordService.getAcademicRecord(studentId, 2024, 1);

        assertThat(result.semesterGrade()).isEqualTo(grade);
        assertThat(result.courses().major()).containsExactly(major);
        assertThat(result.courses().liberal()).containsExactly(liberal);
    }

    @Test
    @DisplayName("전공 과목이 없으면 전공 목록을 빈 리스트로 반환한다")
    void getAcademicRecord_withoutMajorCourses_returnsEmptyMajorList() {
        UUID studentId = UUID.randomUUID();
        SemesterAcademicRecordDto.SemesterGradeResponse grade =
                new SemesterAcademicRecordDto.SemesterGradeResponse(
                        2023, 2, 12, 15, new BigDecimal("3.40"), 10, 150, new BigDecimal("80.1")
                );

        StudentCourseDto.CourseDetailDto liberal = new StudentCourseDto.CourseDetailDto(
                "10", "영어회화", "ENG201", FacultyDivision.중핵, 2, "박교수", "A0", 2,
                false, false, 2023, 2, 90, false
        );

        when(semesterAcademicRecordService.getSemesterGradesByYearAndSemester(studentId, 2023, 2)).thenReturn(grade);
        when(studentCourseService.getStudentCourses(studentId, 2023, 2)).thenReturn(List.of(liberal));

        AcademicRecordResponse result = academicRecordService.getAcademicRecord(studentId, 2023, 2);

        assertThat(result.courses().major()).isEmpty();
        assertThat(result.courses().liberal()).containsExactly(liberal);
    }
}
