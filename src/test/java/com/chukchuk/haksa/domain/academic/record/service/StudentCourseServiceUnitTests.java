package com.chukchuk.haksa.domain.academic.record.service;

import com.chukchuk.haksa.domain.academic.record.dto.StudentCourseDto;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentCourseServiceUnitTests {

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @InjectMocks
    private StudentCourseService studentCourseService;

    @Test
    @DisplayName("수강 과목을 CourseDetailDto로 매핑해 반환한다")
    void getStudentCourses_mapsCourses() {
        UUID studentId = UUID.randomUUID();
        StudentCourse course = mock(StudentCourse.class, RETURNS_DEEP_STUBS);

        when(course.getId()).thenReturn(101L);
        when(course.getOffering().getCourse().getCourseName()).thenReturn("자료구조");
        when(course.getOffering().getCourse().getCourseCode()).thenReturn("CSE101");
        when(course.getOffering().getFacultyDivisionName()).thenReturn(FacultyDivision.전핵);
        when(course.getOffering().getPoints()).thenReturn(3);
        when(course.getOffering().getProfessor().getProfessorName()).thenReturn("홍길동");
        when(course.getGrade().getValue().getValue()).thenReturn("A+");
        when(course.getPoints()).thenReturn(3);
        when(course.getIsRetake()).thenReturn(false);
        when(course.getOffering().getIsVideoLecture()).thenReturn(false);
        when(course.getOffering().getYear()).thenReturn(2024);
        when(course.getOffering().getSemester()).thenReturn(1);
        when(course.getOriginalScore()).thenReturn(95);
        when(course.isRetakeDeleted()).thenReturn(false);

        when(studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, 2024, 1))
                .thenReturn(List.of(course));

        List<StudentCourseDto.CourseDetailDto> result = studentCourseService.getStudentCourses(studentId, 2024, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).courseName()).isEqualTo("자료구조");
        assertThat(result.get(0).courseCode()).isEqualTo("CSE101");
        assertThat(result.get(0).areaType()).isEqualTo(FacultyDivision.전핵);
        assertThat(result.get(0).professor()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("교수/성적/재수강 값이 null이면 DTO 기본값을 사용한다")
    void getStudentCourses_usesDefaultsForNullables() {
        UUID studentId = UUID.randomUUID();
        StudentCourse course = mock(StudentCourse.class, RETURNS_DEEP_STUBS);

        when(course.getId()).thenReturn(102L);
        when(course.getOffering().getCourse().getCourseName()).thenReturn("교양영어");
        when(course.getOffering().getCourse().getCourseCode()).thenReturn("ENG101");
        when(course.getOffering().getFacultyDivisionName()).thenReturn(FacultyDivision.중핵);
        when(course.getOffering().getPoints()).thenReturn(2);
        when(course.getOffering().getProfessor()).thenReturn(null);
        when(course.getGrade()).thenReturn(null);
        when(course.getPoints()).thenReturn(null);
        when(course.getIsRetake()).thenReturn(null);
        when(course.getOffering().getIsVideoLecture()).thenReturn(null);
        when(course.getOffering().getYear()).thenReturn(2023);
        when(course.getOffering().getSemester()).thenReturn(2);
        when(course.getOriginalScore()).thenReturn(null);
        when(course.isRetakeDeleted()).thenReturn(false);

        when(studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, 2023, 2))
                .thenReturn(List.of(course));

        List<StudentCourseDto.CourseDetailDto> result = studentCourseService.getStudentCourses(studentId, 2023, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).professor()).isEqualTo("미지정");
        assertThat(result.get(0).grade()).isEqualTo("F");
        assertThat(result.get(0).score()).isEqualTo(0);
        assertThat(result.get(0).isRetake()).isFalse();
        assertThat(result.get(0).isOnline()).isFalse();
        assertThat(result.get(0).originalScore()).isEqualTo(0);
    }
}
