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
import com.chukchuk.haksa.infrastructure.portal.model.AcademicSummary;
import com.chukchuk.haksa.infrastructure.portal.model.CourseInfo;
import com.chukchuk.haksa.infrastructure.portal.model.GradeSummary;
import com.chukchuk.haksa.infrastructure.portal.model.MergedOfferingAcademic;
import com.chukchuk.haksa.infrastructure.portal.model.OfferingInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalAcademicData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalCurriculumData;
import com.chukchuk.haksa.infrastructure.portal.model.ProfessorInfo;
import com.chukchuk.haksa.infrastructure.portal.model.Ranking;
import com.chukchuk.haksa.infrastructure.portal.model.SemesterCourseInfo;
import com.chukchuk.haksa.infrastructure.portal.model.SemesterGrade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

    @Test
    void mergeOfferingsAndAcademic_mergesByCourseYearSemester() throws Exception {
        PortalCurriculumData curriculumData = new PortalCurriculumData(
                List.of(new CourseInfo(
                        "CSE101",
                        "자료구조",
                        "홍길동",
                        "컴퓨터공학과",
                        3,
                        "A+",
                        false,
                        "월1-2",
                        "전선",
                        100,
                        200,
                        20241,
                        4.3,
                        false
                )),
                List.of(new ProfessorInfo("홍길동")),
                List.of(new OfferingInfo(
                        "CSE101",
                        2024,
                        1,
                        "01",
                        "홍길동",
                        "월1-2",
                        3,
                        "컴퓨터공학과",
                        "전선",
                        20241,
                        100,
                        200,
                        "ABSOLUTE",
                        false
                ))
        );

        SemesterCourseInfo semester = new SemesterCourseInfo(
                2024,
                1,
                List.of(new CourseInfo(
                        "CSE101",
                        "자료구조",
                        "홍길동",
                        "컴퓨터공학과",
                        3,
                        "A+",
                        false,
                        "월1-2",
                        "전선",
                        100,
                        200,
                        20241,
                        4.3,
                        false
                ))
        );
        GradeSummary grades = new GradeSummary(
                List.of(new SemesterGrade(2024, 1, "3", "3", "4.3", 95.0, new Ranking(1, 10))),
                new AcademicSummary(3, 3, 4.3, 95.0)
        );
        PortalAcademicData academicData = new PortalAcademicData(
                List.of(semester),
                grades,
                new AcademicSummary(3, 3, 4.3, 95.0)
        );

        Method method = SyncAcademicRecordService.class
                .getDeclaredMethod("mergeOfferingsAndAcademic", PortalCurriculumData.class, PortalAcademicData.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<Object, MergedOfferingAcademic> merged =
                (Map<Object, MergedOfferingAcademic>) method.invoke(service, curriculumData, academicData);

        assertThat(merged).hasSize(1);
        MergedOfferingAcademic mergedEntry = merged.values().iterator().next();
        assertThat(mergedEntry.getAcademic()).isNotNull();
        assertThat(mergedEntry.getOffering().getEvaluationType()).isEqualTo("ABSOLUTE");
    }
}
