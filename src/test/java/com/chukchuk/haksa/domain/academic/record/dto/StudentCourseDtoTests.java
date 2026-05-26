// 학업 기록 과목 응답 DTO의 포털 원본 이수구분 매핑을 검증한다
package com.chukchuk.haksa.domain.academic.record.dto;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentCourseDtoTests {

    @Test
    @DisplayName("기타 과목 응답은 canonical areaType과 포털 원본 rawAreaType을 함께 반환한다")
    void from_whenEtcCourseHasRawFacultyDivision_returnsRawAreaType() {
        Course course = mock(Course.class);
        when(course.getCourseName()).thenReturn("교직 과목");
        when(course.getCourseCode()).thenReturn("EDU101");

        Professor professor = mock(Professor.class);
        when(professor.getProfessorName()).thenReturn("김교수");

        CourseOffering offering = mock(CourseOffering.class);
        when(offering.getCourse()).thenReturn(course);
        when(offering.getFacultyDivisionName()).thenReturn(FacultyDivision.기타);
        when(offering.getRawFacultyDivisionName()).thenReturn("교직");
        when(offering.getPoints()).thenReturn(2);
        when(offering.getProfessor()).thenReturn(professor);
        when(offering.getIsVideoLecture()).thenReturn(false);
        when(offering.getYear()).thenReturn(2024);
        when(offering.getSemester()).thenReturn(1);

        StudentCourse studentCourse = mock(StudentCourse.class);
        when(studentCourse.getId()).thenReturn(10L);
        when(studentCourse.getOffering()).thenReturn(offering);
        when(studentCourse.getGrade()).thenReturn(new Grade(GradeType.P));
        when(studentCourse.getPoints()).thenReturn(2);
        when(studentCourse.getIsRetake()).thenReturn(false);
        when(studentCourse.getOriginalScore()).thenReturn(80);
        when(studentCourse.isRetakeDeleted()).thenReturn(false);

        StudentCourseDto.CourseDetailDto result = StudentCourseDto.CourseDetailDto.from(studentCourse);

        assertThat(result.areaType()).isEqualTo(FacultyDivision.기타);
        assertThat(result.rawAreaType()).isEqualTo("교직");
    }
}
