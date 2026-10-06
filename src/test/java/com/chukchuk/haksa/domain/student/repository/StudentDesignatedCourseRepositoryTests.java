// 학생 지정과목 repository의 순서 보존과 학생별 삭제를 검증한다.

package com.chukchuk.haksa.domain.student.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentDesignatedCourse;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.infrastructure.portal.model.DesignatedCourseData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentDesignatedCourseRepositoryTests {

  @Autowired private StudentDesignatedCourseRepository designatedCourseRepository;

  @Autowired private StudentRepository studentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private DepartmentRepository departmentRepository;

  @PersistenceContext private EntityManager entityManager;

  @Test
  @DisplayName("학생 지정과목은 원본 순서와 같은 과목 코드 중복을 보존한다")
  void preservesSourceOrderAndDuplicateCourseCodes() {
    Student student = saveStudent();
    List<StudentDesignatedCourse> courses =
        List.of(
            new StudentDesignatedCourse(student, designatedCourse("C101", "첫 번째", 0)),
            new StudentDesignatedCourse(student, designatedCourse("C101", "두 번째", 1)));
    designatedCourseRepository.saveAll(courses);
    entityManager.flush();
    entityManager.clear();

    List<StudentDesignatedCourse> found =
        designatedCourseRepository.findAllByStudentIdOrderBySourceOrder(student.getId());

    assertThat(found).extracting(StudentDesignatedCourse::getSourceOrder).containsExactly(0, 1);
    assertThat(found)
        .extracting(StudentDesignatedCourse::getSubjtNm)
        .containsExactly("첫 번째", "두 번째");
  }

  @Test
  @DisplayName("학생 지정과목 bulk delete는 대상 학생의 행만 삭제한다")
  void deletesOnlyTargetStudentCourses() {
    Student target = saveStudent();
    Student other = saveStudent();
    designatedCourseRepository.save(
        new StudentDesignatedCourse(target, designatedCourse("C101", "대상", 0)));
    designatedCourseRepository.save(
        new StudentDesignatedCourse(other, designatedCourse("C202", "다른 학생", 0)));
    entityManager.flush();

    int deleted = designatedCourseRepository.deleteAllByStudentId(target.getId());

    assertThat(deleted).isEqualTo(1);
    assertThat(designatedCourseRepository.findAllByStudentIdOrderBySourceOrder(target.getId()))
        .isEmpty();
    assertThat(designatedCourseRepository.findAllByStudentIdOrderBySourceOrder(other.getId()))
        .hasSize(1);
  }

  private Student saveStudent() {
    User user =
        userRepository.save(
            User.builder()
                .email(UUID.randomUUID() + "@example.com")
                .profileNickname("tester")
                .build());
    Department department =
        departmentRepository.save(
            new Department("D-" + UUID.randomUUID(), "테스트학과-" + UUID.randomUUID()));
    return studentRepository.save(
        Student.builder()
            .studentCode("S-" + UUID.randomUUID())
            .name("홍길동")
            .department(department)
            .admissionYear(2024)
            .isTransferStudent(false)
            .user(user)
            .build());
  }

  private static DesignatedCourseData designatedCourse(
      String subjectCode, String subjectName, int sourceOrder) {
    return new DesignatedCourseData(
        "01", subjectCode, subjectName, 3, "TRANSFER", 2024, "1학기", "", sourceOrder);
  }
}
