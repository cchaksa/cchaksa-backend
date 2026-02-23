package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.dto.StudentDto;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceUnitTests {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserService userService;

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    @DisplayName("studentId로 학생을 조회할 수 있다")
    void getStudentById_success() {
        UUID studentId = UUID.randomUUID();
        Student student = org.mockito.Mockito.mock(Student.class);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Student found = studentService.getStudentById(studentId);

        assertThat(found).isSameAs(student);
    }

    @Test
    @DisplayName("학생이 없으면 STUDENT_NOT_FOUND 예외를 던진다")
    void getStudentById_notFound_throws() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(studentId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.STUDENT_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("userId로 학생을 조회한다")
    void getStudentByUserId_success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("u@example.com").profileNickname("u").build();
        Student student = org.mockito.Mockito.mock(Student.class);
        user.setStudent(student);
        when(userService.getUserById(userId)).thenReturn(user);

        Student found = studentService.getStudentByUserId(userId);

        assertThat(found).isSameAs(student);
    }

    @Test
    @DisplayName("재연동 마킹 시 학생을 조회해 markReconnected 후 저장한다")
    void markReconnectedByUser_success() {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").profileNickname("u").build();
        Student student = org.mockito.Mockito.mock(Student.class);
        when(studentRepository.findByUser(user)).thenReturn(Optional.of(student));

        studentService.markReconnectedByUser(user);

        verify(student).markReconnected();
        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("재연동 대상 학생이 없으면 STUDENT_NOT_FOUND 예외를 던진다")
    void markReconnectedByUser_notFound_throws() {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").profileNickname("u").build();
        when(studentRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.markReconnectedByUser(user))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.STUDENT_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("학생 저장은 repository.save에 위임한다")
    void save_delegatesToRepository() {
        Student student = org.mockito.Mockito.mock(Student.class);

        studentService.save(student);

        verify(studentRepository).save(student);
    }

    @Test
    @DisplayName("학생 프로필 조회 성공 시 currentSemester를 계산해 반환한다")
    void getStudentProfile_success() {
        UUID studentId = UUID.randomUUID();
        Student student = profileStudent(3, 7, Instant.parse("2026-02-22T00:00:00Z"), null);
        when(studentRepository.findProfileByIdWithAssociations(studentId)).thenReturn(Optional.of(student));

        StudentDto.StudentProfileResponse response = studentService.getStudentProfile(studentId);

        assertThat(response.studentCode()).isEqualTo("20201234");
        assertThat(response.currentSemester()).isEqualTo(2);
        assertThat(response.lastSyncedAt()).isEqualTo("");
    }

    @Test
    @DisplayName("학생 프로필이 없으면 STUDENT_NOT_FOUND 공통 예외를 던진다")
    void getStudentProfile_notFound_throws() {
        UUID studentId = UUID.randomUUID();
        when(studentRepository.findProfileByIdWithAssociations(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentProfile(studentId))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.STUDENT_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("학생 데이터 초기화 시 학기/과목을 reset하고 학업요약을 삭제한다")
    void resetBy_resetsAndDeletesAcademicSummary() {
        UUID studentId = UUID.randomUUID();
        Student student = org.mockito.Mockito.mock(Student.class);
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        studentService.resetBy(studentId);

        verify(student).resetAcademicData();
        verify(studentAcademicRecordRepository).deleteByStudentId(studentId);
    }

    @Test
    @DisplayName("목표 GPA 설정은 repository update 메서드에 위임한다")
    void setStudentTargetGpa_updatesRepository() {
        UUID studentId = UUID.randomUUID();

        studentService.setStudentTargetGpa(studentId, 3.8);

        verify(studentRepository).updateTargetGpaByStudentId(studentId, 3.8);
    }

    private Student profileStudent(int gradeLevel, int completedSemesters, Instant updatedAt, Instant lastSyncedAt) {
        Student student = org.mockito.Mockito.mock(Student.class);
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").profileNickname("user").build();
        Department department = new Department("CS", "컴퓨터학과");
        Department major = new Department("CS", "컴퓨터학과");
        AcademicInfo academicInfo = AcademicInfo.builder()
                .admissionYear(2020)
                .status(StudentStatus.재학)
                .gradeLevel(gradeLevel)
                .completedSemesters(completedSemesters)
                .isTransferStudent(false)
                .build();

        when(student.getStudentCode()).thenReturn("20201234");
        when(student.getName()).thenReturn("홍길동");
        when(student.getDepartment()).thenReturn(department);
        when(student.getMajor()).thenReturn(major);
        when(student.getSecondaryMajor()).thenReturn(null);
        when(student.getAcademicInfo()).thenReturn(academicInfo);
        when(student.getUpdatedAt()).thenReturn(updatedAt);
        when(student.isReconnectionRequired()).thenReturn(false);
        when(student.getUser()).thenReturn(user);
        user.updateLastSyncedAt(lastSyncedAt);
        return student;
    }
}
