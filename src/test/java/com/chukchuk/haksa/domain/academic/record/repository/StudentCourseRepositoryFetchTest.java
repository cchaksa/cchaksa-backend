// 학생 수강 기록 조회 시 개설 과목과 과목 정보가 함께 초기화되는지 검증한다.

package com.chukchuk.haksa.domain.academic.record.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "scraping.scheduler.enabled=false",
      "scraping.publisher.enabled=false",
      "scraping.stale.enabled=false"
    })
@ActiveProfiles("test")
@Transactional
class StudentCourseRepositoryFetchTest {

  @Autowired private StudentCourseRepository studentCourseRepository;

  @Autowired private StudentRepository studentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private DepartmentRepository departmentRepository;

  @Autowired private CourseRepository courseRepository;

  @Autowired private CourseOfferingRepository courseOfferingRepository;

  @PersistenceContext private EntityManager entityManager;

  @Test
  void initializesOfferingAndCourseBeforePersistenceContextIsCleared() {
    Department department = departmentRepository.save(new Department("FETCH-TEST", "조회 테스트 학과"));
    User user =
        userRepository.save(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .profileNickname("fetch-test")
                .build());
    Student student =
        studentRepository.save(
            Student.builder()
                .studentCode("FETCH-" + UUID.randomUUID())
                .name("조회 테스트")
                .department(department)
                .admissionYear(2026)
                .semesterEnrolled(1)
                .isTransferStudent(false)
                .isGraduated(false)
                .status(StudentStatus.재학)
                .gradeLevel(1)
                .completedSemesters(1)
                .admissionType("신입")
                .user(user)
                .build());
    Course course = courseRepository.save(new Course("FETCH101", "fetch join 테스트"));
    CourseOffering offering =
        courseOfferingRepository.save(
            new CourseOffering(
                202601,
                false,
                2026,
                1,
                "조회 테스트 학과",
                "A",
                "월 1교시",
                1,
                3,
                EvaluationType.UNKNOWN,
                FacultyDivision.전선,
                course,
                null,
                department,
                null));
    studentCourseRepository.save(
        new StudentCourse(student, offering, new Grade(GradeType.A0), 3, false, 90, false));
    entityManager.flush();
    entityManager.clear();

    List<StudentCourse> fetched =
        studentCourseRepository.findAllWithCourseByStudentId(student.getId());
    entityManager.clear();

    assertThat(fetched).hasSize(1);
    assertThat(fetched.get(0).getOffering().getCourse().getCourseCode()).isEqualTo("FETCH101");
  }
}
