package com.chukchuk.haksa.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPortalConnectionRepositoryTests {

  @Mock private UserService userService;

  @Mock private StudentService studentService;

  private UserPortalConnectionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new UserPortalConnectionRepository(userService, studentService);
  }

  @Test
  @DisplayName("기존 Student 가 있으면 재사용하여 중복 삽입을 막는다")
  void initializePortalConnectionReusesExistingStudent() {
    UUID userId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    User user =
        User.builder().id(userId).email("test@example.com").profileNickname("tester").build();
    Student existingStudent = mock(Student.class);
    when(existingStudent.getId()).thenReturn(studentId);
    when(studentService.findPortalPendingStudent(userId)).thenReturn(Optional.of(existingStudent));

    repository.initializePortalConnection(user, sampleStudentData());

    verify(studentService).resetForPortalReuse(studentId);
    verify(existingStudent).updateUser(user);
    verify(studentService).save(existingStudent);
    verify(userService).save(user);
    verify(userService).evictUserDetailsCache(userId);
    assertThat(user.getStudent()).isEqualTo(existingStudent);
  }

  @Test
  @DisplayName("기존 Student 가 없으면 새 Student 를 생성한다")
  void initializePortalConnectionCreatesNewStudentWhenAbsent() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder().id(userId).email("test@example.com").profileNickname("tester").build();
    when(studentService.findPortalPendingStudent(userId)).thenReturn(Optional.empty());

    repository.initializePortalConnection(user, sampleStudentData());

    ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
    verify(studentService).save(studentCaptor.capture());
    Student saved = studentCaptor.getValue();
    assertThat(saved.getStudentCode()).isEqualTo("20516041");
    verify(userService).save(user);
    verify(userService).evictUserDetailsCache(userId);
  }

  @Test
  @DisplayName("최초 연동 시 포털 편입생 여부를 Student에 저장한다")
  void initializePortalConnectionStoresTransferStudentFlag() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder().id(userId).email("test@example.com").profileNickname("tester").build();
    when(studentService.findPortalPendingStudent(userId)).thenReturn(Optional.empty());

    repository.initializePortalConnection(user, sampleStudentData(true, "2"));

    ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
    verify(studentService).save(studentCaptor.capture());
    assertThat(studentCaptor.getValue().isTransferStudent()).isTrue();
  }

  @Test
  @DisplayName("새로고침 시 포털 편입생 여부를 최신 값으로 갱신한다")
  void refreshPortalConnectionUpdatesTransferStudentFlag() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder().id(userId).email("test@example.com").profileNickname("tester").build();
    Student student = mock(Student.class);
    StudentInitializationDataType studentData = sampleStudentData(true, "2");
    when(studentService.getStudentByUserId(userId)).thenReturn(student);
    when(student.needsUpdate(studentData)).thenReturn(true);

    repository.refreshPortalConnection(user, studentData);

    verify(student)
        .updateInfo(
            eq("홍길동"),
            eq(studentData.getDepartment()),
            eq(studentData.getMajor()),
            isNull(),
            eq(2020),
            eq(10),
            eq(true),
            eq(false),
            eq(StudentStatus.재학),
            eq(4),
            eq(8),
            eq("2"));
    verify(userService).save(user);
  }

  private StudentInitializationDataType sampleStudentData() {
    return sampleStudentData(false, "정시");
  }

  private StudentInitializationDataType sampleStudentData(
      boolean isTransferStudent, String admissionType) {
    Department department = new Department("D1", "컴퓨터학부");
    return StudentInitializationDataType.builder()
        .studentCode("20516041")
        .name("홍길동")
        .department(department)
        .major(department)
        .secondaryMajor(null)
        .admissionYear(2020)
        .semesterEnrolled(10)
        .isTransferStudent(isTransferStudent)
        .isGraduated(false)
        .status(StudentStatus.재학)
        .gradeLevel(4)
        .completedSemesters(8)
        .admissionType(admissionType)
        .build();
  }
}
