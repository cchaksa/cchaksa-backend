// 학업 기록 과목 응답 DTO의 포털 원본 이수구분 매핑을 검증한다
package com.chukchuk.haksa.domain.academic.record.dto;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.LiberalArtsAreaCode;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentCourseDtoTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Test
    @DisplayName("수강 과목 응답은 개인 이수 학점을 반환한다")
    void from_returnsStudentSpecificPoints() {
        StudentCourse studentCourse = studentCourse(offering(FacultyDivision.전핵, null));
        when(studentCourse.getPoints()).thenReturn(21);

        StudentCourseDto.CourseDetailDto result = StudentCourseDto.CourseDetailDto.from(studentCourse);

        assertThat(result.credits()).isEqualTo(21);
    }

    @Test
    @DisplayName("선교 과목 응답은 liberalAreaCode 를 반환한다")
    void from_whenMissionCourseHasAreaCode_returnsLiberalAreaCode() {
        CourseOffering offering = offering(FacultyDivision.선교, areaCode(7));
        StudentCourse studentCourse = studentCourse(offering);

        StudentCourseDto.CourseDetailDto result = StudentCourseDto.CourseDetailDto.from(studentCourse);

        assertThat(result.areaType()).isEqualTo(FacultyDivision.선교);
        assertThat(result.liberalAreaCode()).isEqualTo(7);
    }

    @Test
    @DisplayName("선교가 아닌 과목 응답은 liberalAreaCode 를 반환하지 않는다")
    void from_whenNonMissionCourseHasAreaCode_omitsLiberalAreaCode() {
        CourseOffering offering = offering(FacultyDivision.중핵, areaCode(7));
        StudentCourse studentCourse = studentCourse(offering);

        StudentCourseDto.CourseDetailDto result = StudentCourseDto.CourseDetailDto.from(studentCourse);

        assertThat(result.areaType()).isEqualTo(FacultyDivision.중핵);
        assertThat(result.liberalAreaCode()).isNull();
    }

    @Test
    @DisplayName("liberalAreaCode 가 null 이면 JSON 응답에서 키가 omit 된다")
    void serializesWithoutLiberalAreaCodeWhenNull() throws JsonProcessingException {
        CourseOffering offering = offering(FacultyDivision.중핵, areaCode(7));
        StudentCourse studentCourse = studentCourse(offering);

        StudentCourseDto.CourseDetailDto result = StudentCourseDto.CourseDetailDto.from(studentCourse);
        String json = objectMapper.writeValueAsString(result);

        assertThat(json).doesNotContain("liberalAreaCode");
    }

    private CourseOffering offering(FacultyDivision division, LiberalArtsAreaCode areaCode) {
        Course course = mock(Course.class);
        when(course.getCourseName()).thenReturn("기독교의 이해");
        when(course.getCourseCode()).thenReturn("GEN001");

        Professor professor = mock(Professor.class);
        when(professor.getProfessorName()).thenReturn("김교수");

        CourseOffering offering = mock(CourseOffering.class);
        when(offering.getCourse()).thenReturn(course);
        when(offering.getFacultyDivisionName()).thenReturn(division);
        when(offering.getRawFacultyDivisionName()).thenReturn(null);
        when(offering.getPoints()).thenReturn(3);
        when(offering.getProfessor()).thenReturn(professor);
        when(offering.getIsVideoLecture()).thenReturn(false);
        when(offering.getYear()).thenReturn(2024);
        when(offering.getSemester()).thenReturn(1);
        when(offering.getLiberalArtsAreaCode()).thenReturn(areaCode);
        return offering;
    }

    private LiberalArtsAreaCode areaCode(Integer code) {
        LiberalArtsAreaCode areaCode = mock(LiberalArtsAreaCode.class);
        when(areaCode.getCode()).thenReturn(code);
        return areaCode;
    }

    private StudentCourse studentCourse(CourseOffering offering) {
        StudentCourse studentCourse = mock(StudentCourse.class);
        when(studentCourse.getId()).thenReturn(11L);
        when(studentCourse.getOffering()).thenReturn(offering);
        when(studentCourse.getGrade()).thenReturn(new Grade(GradeType.A_PLUS));
        when(studentCourse.getPoints()).thenReturn(3);
        when(studentCourse.getIsRetake()).thenReturn(false);
        when(studentCourse.getOriginalScore()).thenReturn(95);
        when(studentCourse.isRetakeDeleted()).thenReturn(false);
        return studentCourse;
    }
}
