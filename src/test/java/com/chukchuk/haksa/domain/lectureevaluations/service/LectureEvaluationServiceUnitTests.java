package com.chukchuk.haksa.domain.lectureevaluations.service;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.lectureevaluations.config.LectureEvaluationProperties;
import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationRepository;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class LectureEvaluationServiceUnitTests {

    private final StudentService studentService = mock(StudentService.class);
    private final SemesterAcademicRecordRepository semesterAcademicRecordRepository = mock(SemesterAcademicRecordRepository.class);
    private final StudentCourseRepository studentCourseRepository = mock(StudentCourseRepository.class);
    private final CourseEvaluationRepository courseEvaluationRepository = mock(CourseEvaluationRepository.class);
    private final LectureEvaluationProperties properties = new LectureEvaluationProperties(2026, 10);

    private final LectureEvaluationService service = new LectureEvaluationService(
            studentService,
            semesterAcademicRecordRepository,
            studentCourseRepository,
            courseEvaluationRepository,
            properties
    );

    @Test
    @DisplayName("target 학기가 평가 필요 상태이면 IP 과목을 제외하고 score null을 유지한다")
    void getRequired_returnsTargetGradesExcludingIpAndKeepsNullScore() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(studentId);
        when(studentService.getStudentByUserId(userId)).thenReturn(student);

        SemesterAcademicRecord record = mock(SemesterAcademicRecord.class);
        when(record.isLectureEvaluationRequired()).thenReturn(true);
        when(record.isLectureEvaluationCompleted()).thenReturn(false);
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(Optional.of(record));

        StudentCourse completed = studentCourse(1L, "CSE101", "컴퓨터네트워크", 11L, "김민규", GradeType.A_PLUS, null);
        StudentCourse inProgress = studentCourse(2L, "CSE102", "운영체제", 12L, "이교수", GradeType.IP, 95);
        when(studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(List.of(completed, inProgress));

        LectureEvaluationDto.RequiredResponse response = service.getRequired(userId);

        assertThat(response.lectureEvaluationRequired()).isTrue();
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.semester()).isEqualTo(10);
        assertThat(response.grades()).hasSize(1);
        assertThat(response.grades().get(0).courseId()).isEqualTo(1L);
        assertThat(response.grades().get(0).professorId()).isEqualTo(11L);
        assertThat(response.grades().get(0).score()).isNull();
    }

    @Test
    @DisplayName("target 학기 row가 없으면 false와 빈 grades를 반환한다")
    void getRequired_returnsFalseWhenTargetSemesterRecordMissing() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(studentId);
        when(studentService.getStudentByUserId(userId)).thenReturn(student);
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(Optional.empty());

        LectureEvaluationDto.RequiredResponse response = service.getRequired(userId);

        assertThat(response.lectureEvaluationRequired()).isFalse();
        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.semester()).isEqualTo(10);
        assertThat(response.grades()).isEmpty();
        verify(studentCourseRepository, never()).findByStudentIdAndYearAndSemester(any(), any(), any());
    }

    @Test
    @DisplayName("제출 과목이 평가 대상 전체와 일치하면 저장하고 학기 완료 상태로 변경한다")
    void submit_savesEvaluationsAndMarksSemesterCompleted() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(studentId);
        when(studentService.getStudentByUserId(userId)).thenReturn(student);

        SemesterAcademicRecord record = mock(SemesterAcademicRecord.class);
        when(record.isLectureEvaluationRequired()).thenReturn(true);
        when(record.isLectureEvaluationCompleted()).thenReturn(false);
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(Optional.of(record));

        StudentCourse target = studentCourse(1L, "CSE101", "컴퓨터네트워크", 11L, "김민규", GradeType.A_PLUS, 88);
        when(studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(List.of(target));

        LectureEvaluationDto.SubmitRequest request = new LectureEvaluationDto.SubmitRequest(
                2026,
                10,
                List.of(new LectureEvaluationDto.SubmitEvaluation(
                        1L,
                        11L,
                        List.of("LOW_ASSIGNMENT"),
                        "과제가 적어요"
                ))
        );

        service.submit(userId, request);

        verify(courseEvaluationRepository).saveAll(anyList());
        verify(record).markLectureEvaluationCompleted();
    }

    @Test
    @DisplayName("제출 과목이 평가 대상과 다르면 예외를 던진다")
    void submit_throwsWhenSubmittedCoursesDoNotMatchTargets() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(studentId);
        when(studentService.getStudentByUserId(userId)).thenReturn(student);

        SemesterAcademicRecord record = mock(SemesterAcademicRecord.class);
        when(record.isLectureEvaluationRequired()).thenReturn(true);
        when(record.isLectureEvaluationCompleted()).thenReturn(false);
        when(semesterAcademicRecordRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(Optional.of(record));

        StudentCourse target = studentCourse(1L, "CSE101", "컴퓨터네트워크", 11L, "김민규", GradeType.A_PLUS, 88);
        when(studentCourseRepository.findByStudentIdAndYearAndSemester(studentId, 2026, 10))
                .thenReturn(List.of(target));

        LectureEvaluationDto.SubmitRequest request = new LectureEvaluationDto.SubmitRequest(
                2026,
                10,
                List.of(new LectureEvaluationDto.SubmitEvaluation(
                        999L,
                        11L,
                        List.of(),
                        null
                ))
        );

        assertThatThrownBy(() -> service.submit(userId, request))
                .isInstanceOf(CommonException.class);
        verify(courseEvaluationRepository, never()).saveAll(anyList());
    }

    private StudentCourse studentCourse(
            Long courseId,
            String courseCode,
            String courseName,
            Long professorId,
            String professorName,
            GradeType gradeType,
            Integer originalScore
    ) {
        StudentCourse studentCourse = mock(StudentCourse.class);
        CourseOffering offering = mock(CourseOffering.class);
        Course course = mock(Course.class);
        Professor professor = mock(Professor.class);

        when(studentCourse.getOffering()).thenReturn(offering);
        when(studentCourse.getGrade()).thenReturn(new Grade(gradeType));
        when(studentCourse.getOriginalScore()).thenReturn(originalScore);
        when(offering.getCourse()).thenReturn(course);
        when(offering.getProfessor()).thenReturn(professor);
        when(offering.getFacultyDivisionName()).thenReturn(FacultyDivision.전선);
        when(offering.getPoints()).thenReturn(3);
        when(course.getId()).thenReturn(courseId);
        when(course.getCourseCode()).thenReturn(courseCode);
        when(course.getCourseName()).thenReturn(courseName);
        when(professor.getId()).thenReturn(professorId);
        when(professor.getProfessorName()).thenReturn(professorName);

        return studentCourse;
    }
}
