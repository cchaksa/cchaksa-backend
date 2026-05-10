package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.dto.StudentDto;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.student.model.embeddable.AcademicInfo;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
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
    private UserRepository userRepository;

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @Mock
    private SemesterAcademicRecordRepository semesterAcademicRecordRepository;

    @Mock
    private StudentCourseRepository studentCourseRepository;

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
    @DisplayName("학생 컨텍스트가 필요할 때 userId로 studentId를 조회한다")
    void getRequiredStudentIdByUserId_success() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        User user = User.builder().id(userId).email("u@example.com").profileNickname("u").build();
        Student student = org.mockito.Mockito.mock(Student.class);
        user.setStudent(student);
        when(userService.getUserById(userId)).thenReturn(user);
        when(student.getId()).thenReturn(studentId);

        UUID found = studentService.getRequiredStudentIdByUserId(userId);

        assertThat(found).isEqualTo(studentId);
    }

    @Test
    @DisplayName("학생 컨텍스트가 없으면 USER_NOT_CONNECTED 예외를 던진다")
    void getRequiredStudentIdByUserId_notConnected_throws() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("u@example.com").profileNickname("u").build();
        when(userService.getUserById(userId)).thenReturn(user);

        assertThatThrownBy(() -> studentService.getRequiredStudentIdByUserId(userId))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_CONNECTED.code()));
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
    @DisplayName("userId 기준 프로필 조회 성공 시 현재 연결된 학생 프로필을 반환한다")
    void getStudentProfileByUserId_success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        Student student = profileStudent(3, 6, Instant.parse("2026-02-22T00:00:00Z"),
                Instant.parse("2026-02-22T12:00:00Z"), "경영학과");
        user.setStudent(student);
        user.updateLastSyncedAt(Instant.parse("2026-02-22T12:00:00Z"));

        when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));

        StudentDto.StudentProfileResponse response = studentService.getStudentProfileByUserId(userId);

        assertThat(response.studentCode()).isEqualTo("20201234");
        assertThat(response.currentSemester()).isEqualTo(2);
        assertThat(response.dualMajorName()).isEqualTo("경영학과");
        assertThat(response.lastSyncedAt()).isEqualTo("2026-02-22T12:00:00Z");
    }

    @Test
    @DisplayName("프로필 조회 대상 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void getStudentProfileByUserId_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentProfileByUserId(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .satisfies(ex -> assertThat(((EntityNotFoundException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_FOUND.code()));
    }

    @Test
    @DisplayName("프로필 조회 대상 사용자가 학생과 연결되지 않았으면 USER_NOT_CONNECTED 예외를 던진다")
    void getStudentProfileByUserId_userNotConnected_throws() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> studentService.getStudentProfileByUserId(userId))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.USER_NOT_CONNECTED.code()));
    }

    @Test
    @DisplayName("프로필 응답의 lastUpdatedAt과 lastSyncedAt은 null일 때 빈 문자열로 내려간다")
    void getStudentProfileByUserId_nullTimestamps_returnsEmptyString() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("user@example.com").profileNickname("user").build();
        Student student = profileStudent(3, 5, null, null);
        user.setStudent(student);

        when(userRepository.findProfileByIdWithAssociations(userId)).thenReturn(Optional.of(user));

        StudentDto.StudentProfileResponse response = studentService.getStudentProfileByUserId(userId);

        assertThat(response.lastUpdatedAt()).isEmpty();
        assertThat(response.lastSyncedAt()).isEmpty();
    }

    @Test
    @DisplayName("학생 데이터 초기화 시 학기/과목/학업요약을 벌크 삭제한다")
    void resetBy_deletesAcademicRecordsInBulk() {
        UUID studentId = UUID.randomUUID();

        studentService.resetBy(studentId);

        verify(studentCourseRepository).deleteByStudentId(studentId);
        verify(semesterAcademicRecordRepository).deleteByStudentId(studentId);
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
        return profileStudent(gradeLevel, completedSemesters, updatedAt, lastSyncedAt, null);
    }

    private Student profileStudent(int gradeLevel, int completedSemesters, Instant updatedAt, Instant lastSyncedAt,
                                   String secondaryMajorName) {
        Student student = org.mockito.Mockito.mock(Student.class);
        User user = User.builder().id(UUID.randomUUID()).email("user@example.com").profileNickname("user").build();
        Department department = new Department("CS", "컴퓨터학과");
        Department major = new Department("CS", "컴퓨터학과");
        Department secondaryMajor = secondaryMajorName != null ? new Department("BUS", secondaryMajorName) : null;
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
        when(student.getSecondaryMajor()).thenReturn(secondaryMajor);
        when(student.getAcademicInfo()).thenReturn(academicInfo);
        when(student.getUpdatedAt()).thenReturn(updatedAt);
        when(student.isReconnectionRequired()).thenReturn(false);
        org.mockito.Mockito.lenient().when(student.getUser()).thenReturn(user);
        user.updateLastSyncedAt(lastSyncedAt);
        return student;
    }
}
