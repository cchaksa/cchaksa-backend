package com.chukchuk.haksa.domain.user.service;

import com.chukchuk.haksa.domain.academic.record.model.SemesterAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentAcademicRecord;
import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.global.security.cache.AuthTokenCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @SpyBean
    private StudentRepository studentRepository;
    @SpyBean
    private StudentAcademicRecordRepository studentAcademicRecordRepository;
    @Autowired
    private SemesterAcademicRecordRepository semesterAcademicRecordRepository;
    @Autowired
    private StudentCourseRepository studentCourseRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @MockBean
    private AcademicCache academicCache;
    @MockBean
    private AuthTokenCache authTokenCache;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("사용자가 탈퇴하면 관련된 학생 및 학적 정보도 삭제된다")
    void deleteUser_removesStudentAndAllAssociations() {
        User user = userRepository.save(User.builder()
                .email("test@haksa.com")
                .profileNickname("tester")
                .build());
        Student student = createStudent(user);

        persistStudentAssociations(student);

        entityManager.flush();
        entityManager.clear();

        userService.deleteUserById(user.getId());

        entityManager.flush();
        entityManager.clear();

        UUID studentId = student.getId();
        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(studentRepository.findById(studentId)).isEmpty();
        assertThat(studentAcademicRecordRepository.findByStudentId(studentId)).isEmpty();
        assertThat(semesterAcademicRecordRepository.findByStudentId(studentId)).isEmpty();
        assertThat(studentCourseRepository.findAll()).isEmpty();

        verify(academicCache).deleteAllByStudentId(studentId);
        verify(authTokenCache).evictByUserId(user.getId().toString());
    }

    @Test
    @DisplayName("연동하지 않은 사용자의 탈퇴에서는 학생 관련 정보는 아무 처리 되지 않는다")
    void deleteUser_withoutStudent_doesNotFail() {
        User user = userRepository.save(User.builder()
                .email("orphan@haksa.com")
                .profileNickname("orphan")
                .build());

        entityManager.flush();
        entityManager.clear();

        userService.deleteUserById(user.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(user.getId())).isEmpty();
        verify(academicCache, never()).deleteAllByStudentId(any());
        verify(authTokenCache).evictByUserId(user.getId().toString());
        verify(studentRepository, never()).delete(any());
        verify(studentAcademicRecordRepository, never()).deleteByStudentId(any());
    }

    private Student createStudent(User user) {
        Department department = departmentRepository.save(new Department("2000513", "컴퓨터학과"));
        Student student = Student.builder()
                .studentCode("20260001")
                .name("학생")
                .department(department)
                .major(null)
                .secondaryMajor(null)
                .admissionYear(2024)
                .semesterEnrolled(1)
                .isTransferStudent(false)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(1)
                .completedSemesters(0)
                .admissionType("수시")
                .user(user)
                .build();
        return studentRepository.save(student);
    }

    private void persistStudentAssociations(Student student) {
        studentAcademicRecordRepository.save(new StudentAcademicRecord(
                student,
                30,
                24,
                BigDecimal.valueOf(3.8),
                BigDecimal.valueOf(85)
        ));

        SemesterAcademicRecord semesterRecord = new SemesterAcademicRecord(
                student,
                2024,
                1,
                15,
                15,
                BigDecimal.valueOf(3.9),
                BigDecimal.valueOf(88),
                BigDecimal.valueOf(3.9),
                1,
                30
        );
        student.addSemesterRecord(semesterRecord);
        semesterAcademicRecordRepository.save(semesterRecord);

        Course course = courseRepository.save(new Course("CS101", "자료구조"));
        CourseOffering offering = courseOfferingRepository.save(new CourseOffering(
                1,
                false,
                2024,
                1,
                "공과대학",
                "A",
                "월1",
                null,
                3,
                EvaluationType.ABSOLUTE,
                FacultyDivision.전선,
                course,
                null,
                student.getDepartment(),
                null
        ));

        StudentCourse studentCourse = new StudentCourse(
                student,
                offering,
                new Grade(GradeType.A0),
                3,
                false,
                95,
                false
        );
        student.addStudentCourse(studentCourse);
        studentCourseRepository.save(studentCourse);
    }
}
