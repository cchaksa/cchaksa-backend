package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.enrollment.CourseEnrollment;
import com.chukchuk.haksa.application.academic.repository.AcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.service.CourseOfferingService;
import com.chukchuk.haksa.domain.course.service.CourseService;
import com.chukchuk.haksa.domain.professor.service.ProfessorService;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncAcademicRecordServiceTest {

    @Mock
    private AcademicRecordRepository academicRecordRepository;
    @Mock
    private StudentCourseRepository studentCourseRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private CourseOfferingService courseOfferingService;
    @Mock
    private ProfessorService professorService;
    @Mock
    private CourseService courseService;

    @InjectMocks
    private SyncAcademicRecordService service;

    @Test
    void removeDeletedEnrollmentsUsesBatchDelete() {
        StudentCourse existing = mock(StudentCourse.class);
        CourseOffering offering = mock(CourseOffering.class);
        when(offering.getId()).thenReturn(1L);
        when(existing.getOffering()).thenReturn(offering);
        when(existing.getId()).thenReturn(10L);

        List<StudentCourse> existingEnrollments = List.of(existing);
        List<CourseEnrollment> newEnrollments = List.of(
                new CourseEnrollment(UUID.randomUUID(), 2L, new Grade(GradeType.A0), 3, false, 95.0, false)
        );

        int removed = service.removeDeletedEnrollments(mock(Student.class), newEnrollments, existingEnrollments);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentCourseRepository).deleteAllByIdInBatch(captor.capture());
        assertThat(captor.getValue()).containsExactly(10L);
        assertThat(removed).isEqualTo(1);
    }
}
