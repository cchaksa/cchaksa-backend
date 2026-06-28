package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.academic.record.model.LectureEvaluationStatus;
import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.lectureevaluations.model.CourseEvaluation;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationRepository;
import com.chukchuk.haksa.domain.lectureevaluations.repository.CourseEvaluationTagRepository;
import com.chukchuk.haksa.domain.professor.model.Professor;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTestLectureEvaluationServiceUnitTests {

    private static final UUID TARGET_USER_ID = UUID.fromString("faf9c30a-9674-4624-8855-6d0be23c749b");
    private static final UUID TARGET_STUDENT_ID = UUID.fromString("47f72b79-a3f0-4834-869b-8ba3a0cf3474");
    private static final int TARGET_YEAR = 2026;
    private static final int TARGET_SEMESTER = 10;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SemesterAcademicRecordRepository semesterAcademicRecordRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private CourseEvaluationRepository courseEvaluationRepository;

    @Mock
    private CourseEvaluationTagRepository courseEvaluationTagRepository;

    @Mock
    private AcademicCache academicCache;

    @InjectMocks
    private AdminTestLectureEvaluationService service;

    @Test
    @DisplayName("empty-semester는 대상 학기 데이터를 모두 삭제하고 추가 row를 만들지 않는다")
    void setEmptySemester_deletesTargetSemesterDataOnly() {
        Student student = targetStudent();
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));

        service.setEmptySemester();

        InOrder inOrder = inOrder(
                courseEvaluationTagRepository,
                courseEvaluationRepository,
                studentCourseRepository,
                semesterAcademicRecordRepository
        );
        inOrder.verify(courseEvaluationTagRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(courseEvaluationRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(studentCourseRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(semesterAcademicRecordRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        verify(semesterAcademicRecordRepository, never()).save(any());
        verify(studentCourseRepository, never()).saveAll(any());
        verify(academicCache).deleteAllByStudentId(TARGET_STUDENT_ID);
    }

    @Test
    @DisplayName("not-released는 실제 교수 연결 강의를 IP 수강 과목으로 재구성한다")
    void setNotReleased_rebuildsIpCoursesAndNotReleasedSemester() {
        Student student = targetStudent();
        CourseOffering offering = offering(1L, 11L, "자료구조", "김교수");
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));
        when(courseOfferingRepository.findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER))
                .thenReturn(List.of(offering));

        service.setNotReleased();

        assertRebuiltSemester(LectureEvaluationStatus.NOT_RELEASED);
        ArgumentCaptor<List<StudentCourse>> coursesCaptor = ArgumentCaptor.forClass(List.class);
        verify(studentCourseRepository).saveAll(coursesCaptor.capture());
        assertThat(coursesCaptor.getValue()).hasSize(1);
        assertThat(coursesCaptor.getValue().get(0).getOffering()).isSameAs(offering);
        assertThat(coursesCaptor.getValue().get(0).getGrade().getValue()).isEqualTo(GradeType.IP);
        verify(courseEvaluationRepository, never()).saveAll(any());
        verify(academicCache).deleteAllByStudentId(TARGET_STUDENT_ID);
    }

    @Test
    @DisplayName("pending은 완료 성적 수강 과목과 PENDING 학기 row만 재구성한다")
    void setPending_rebuildsCompletedCoursesAndPendingSemesterWithoutEvaluations() {
        Student student = targetStudent();
        CourseOffering offering = offering(1L, 11L, "자료구조", "김교수");
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));
        when(courseOfferingRepository.findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER))
                .thenReturn(List.of(offering));

        service.setPending();

        assertRebuiltSemester(LectureEvaluationStatus.PENDING);
        ArgumentCaptor<List<StudentCourse>> coursesCaptor = ArgumentCaptor.forClass(List.class);
        verify(studentCourseRepository).saveAll(coursesCaptor.capture());
        assertThat(coursesCaptor.getValue().get(0).getGrade().getValue()).isEqualTo(GradeType.A_PLUS);
        verify(courseEvaluationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("skipped는 완료 성적 수강 과목과 SKIPPED 학기 row만 재구성한다")
    void setSkipped_rebuildsCompletedCoursesAndSkippedSemesterWithoutEvaluations() {
        Student student = targetStudent();
        CourseOffering offering = offering(1L, 11L, "자료구조", "김교수");
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));
        when(courseOfferingRepository.findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER))
                .thenReturn(List.of(offering));

        service.setSkipped();

        assertRebuiltSemester(LectureEvaluationStatus.SKIPPED);
        verify(courseEvaluationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("completed는 완료 성적 수강 과목과 평가 데이터를 재구성한다")
    void setCompleted_rebuildsCoursesSemesterAndEvaluations() {
        Student student = targetStudent();
        CourseOffering first = offering(1L, 11L, "자료구조", "김교수");
        CourseOffering second = offering(2L, 12L, "운영체제", "이교수");
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));
        when(courseOfferingRepository.findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER))
                .thenReturn(List.of(first, second));

        service.setCompleted();

        assertRebuiltSemester(LectureEvaluationStatus.COMPLETED);
        ArgumentCaptor<List<CourseEvaluation>> evaluationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseEvaluationRepository).saveAll(evaluationsCaptor.capture());
        assertThat(evaluationsCaptor.getValue()).hasSize(2);
        assertThat(evaluationsCaptor.getValue())
                .allSatisfy(evaluation -> assertThat(evaluation.getTags()).isNotEmpty());
    }

    @Test
    @DisplayName("재사용할 실제 교수 연결 강의가 없으면 임의 데이터를 만들지 않고 실패한다")
    void setPending_throwsWhenReusableOfferingMissing() {
        Student student = targetStudent();
        when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(student.getUser()));
        when(courseOfferingRepository.findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.setPending())
                .isInstanceOf(CommonException.class)
                .hasMessage(ErrorCode.INVALID_ARGUMENT.message());

        InOrder offeringOrder = inOrder(courseOfferingRepository);
        offeringOrder.verify(courseOfferingRepository)
                .normalizeUnsupportedEvaluationTypes(TARGET_YEAR, TARGET_SEMESTER);
        offeringOrder.verify(courseOfferingRepository)
                .findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER);
        verify(studentCourseRepository, never()).saveAll(any());
        verify(semesterAcademicRecordRepository, never()).save(any());
        verify(courseEvaluationRepository, never()).saveAll(any());
    }

    private void assertRebuiltSemester(LectureEvaluationStatus expectedStatus) {
        InOrder inOrder = inOrder(
                courseEvaluationTagRepository,
                courseEvaluationRepository,
                studentCourseRepository,
                semesterAcademicRecordRepository
        );
        inOrder.verify(courseEvaluationTagRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(courseEvaluationRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(studentCourseRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);
        inOrder.verify(semesterAcademicRecordRepository)
                .deleteByStudentIdAndYearAndSemester(TARGET_STUDENT_ID, TARGET_YEAR, TARGET_SEMESTER);

        InOrder offeringOrder = inOrder(courseOfferingRepository);
        offeringOrder.verify(courseOfferingRepository)
                .normalizeUnsupportedEvaluationTypes(TARGET_YEAR, TARGET_SEMESTER);
        offeringOrder.verify(courseOfferingRepository)
                .findReusableLectureEvaluationTestOfferings(TARGET_YEAR, TARGET_SEMESTER);

        ArgumentCaptor<SemesterAcademicRecord> recordCaptor = ArgumentCaptor.forClass(SemesterAcademicRecord.class);
        verify(semesterAcademicRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getStudent().getId()).isEqualTo(TARGET_STUDENT_ID);
        assertThat(recordCaptor.getValue().getYear()).isEqualTo(TARGET_YEAR);
        assertThat(recordCaptor.getValue().getSemester()).isEqualTo(TARGET_SEMESTER);
        assertThat(recordCaptor.getValue().getLectureEvaluationStatus()).isEqualTo(expectedStatus);
    }

    private Student targetStudent() {
        User user = User.builder()
                .id(TARGET_USER_ID)
                .email("front-dev@example.com")
                .profileNickname("front-dev")
                .build();
        Student student = Student.builder()
                .studentCode("20261234")
                .name("프론트테스트")
                .department(new Department("CSE", "컴퓨터학과"))
                .major(new Department("CSE", "컴퓨터학과"))
                .secondaryMajor(null)
                .admissionYear(2024)
                .semesterEnrolled(1)
                .isTransferStudent(false)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(1)
                .completedSemesters(0)
                .admissionType("신입")
                .user(user)
                .build();
        ReflectionTestUtils.setField(student, "id", TARGET_STUDENT_ID);
        user.setStudent(student);
        return student;
    }

    private CourseOffering offering(Long courseId, Long professorId, String courseName, String professorName) {
        Course course = new Course("CSE" + courseId, courseName);
        ReflectionTestUtils.setField(course, "id", courseId);
        Professor professor = new Professor(professorName);
        ReflectionTestUtils.setField(professor, "id", professorId);
        CourseOffering offering = new CourseOffering(
                20261,
                false,
                TARGET_YEAR,
                TARGET_SEMESTER,
                "컴퓨터학과",
                "01",
                null,
                null,
                3,
                EvaluationType.UNKNOWN,
                FacultyDivision.전선,
                course,
                professor,
                null,
                null
        );
        ReflectionTestUtils.setField(offering, "id", courseId * 100);
        return offering;
    }
}
