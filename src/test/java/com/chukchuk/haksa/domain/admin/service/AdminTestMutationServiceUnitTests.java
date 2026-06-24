// dev 테스트 데이터 수정 서비스 동작을 검증하는 테스트
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTestMutationServiceUnitTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

    @Mock
    private AcademicCache academicCache;

    @InjectMocks
    private AdminTestMutationService mutationService;

    @Test
    @DisplayName("현재 인증 계정의 강의 데이터를 추가하고 삭제한다")
    void updateGraduationCourses_addsAndRemovesCourses() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        Student student = student(user);
        user.setStudent(student);
        CourseOffering offering = offering();
        AdminTestDto.UpdateGraduationCoursesRequest request = new AdminTestDto.UpdateGraduationCoursesRequest(
                FacultyDivision.전핵,
                List.of(10L),
                List.of(20L),
                "A+",
                3,
                false,
                null
        );
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courseOfferingRepository.findAllById(List.of(10L))).thenReturn(List.of(offering));

        mutationService.updateGraduationCourses(userId, request);

        ArgumentCaptor<StudentCourse> captor = ArgumentCaptor.forClass(StudentCourse.class);
        verify(studentCourseRepository).save(captor.capture());
        verify(studentCourseRepository).deleteOwnedByStudentIdAndIdIn(student.getId(), List.of(20L));
        verify(academicCache).deleteAllByStudentId(student.getId());
        assertThat(captor.getValue().getStudent()).isSameAs(student);
        assertThat(captor.getValue().getOffering()).isSameAs(offering);
        assertThat(captor.getValue().getPoints()).isEqualTo(3);
    }

    @Test
    @DisplayName("현재 인증 계정의 주전공과 복수전공을 변경한다")
    void updateMajor_changesMajorAndSecondaryMajor() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        Student student = student(user);
        user.setStudent(student);
        Department major = new Department("CSE", "컴퓨터학과");
        Department secondaryMajor = new Department("BUS", "경영학과");
        AdminTestDto.UpdateMajorRequest request = new AdminTestDto.UpdateMajorRequest(1L, true, 2L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(major));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(secondaryMajor));

        mutationService.updateMajor(userId, request);

        assertThat(student.getMajor()).isSameAs(major);
        assertThat(student.getSecondaryMajor()).isSameAs(secondaryMajor);
        verify(studentRepository).save(student);
        verify(academicCache).deleteAllByStudentId(student.getId());
    }

    @Test
    @DisplayName("현재 인증 계정의 테스트 데이터를 초기화한다")
    void resetCurrentAccount_deletesCoursesAndResetsMajors() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        Student student = student(user);
        user.setStudent(student);
        Department otherMajor = new Department("BUS", "경영학과");
        Department secondaryMajor = new Department("ART", "예술학과");
        student.updateMajors(otherMajor, secondaryMajor);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mutationService.resetCurrentAccount(userId);

        verify(studentCourseRepository).deleteByStudentId(student.getId());
        assertThat(student.getMajor()).isSameAs(student.getDepartment());
        assertThat(student.getSecondaryMajor()).isNull();
        verify(studentRepository).save(student);
        verify(academicCache).deleteAllByStudentId(student.getId());
    }

    private static Student student(User user) {
        UUID studentId = UUID.randomUUID();
        Department department = new Department("CSE", "컴퓨터학과");
        Student student = Student.builder()
                .studentCode("20241234")
                .name("테스트")
                .department(department)
                .major(department)
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
        ReflectionTestUtils.setField(student, "id", studentId);
        return student;
    }

    private static CourseOffering offering() {
        Course course = new Course("CSE101", "자료구조");
        return new CourseOffering(
                20241,
                false,
                2024,
                10,
                "컴퓨터학과",
                "01",
                "월 1-2",
                null,
                3,
                EvaluationType.UNKNOWN,
                FacultyDivision.전핵,
                course,
                null,
                null,
                null
        );
    }
}
