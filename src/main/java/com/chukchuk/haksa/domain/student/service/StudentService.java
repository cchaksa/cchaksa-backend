package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.academic.record.repository.SemesterAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentAcademicRecordRepository;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.student.dto.StudentDto;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentDesignatedCourseRepository;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학생 조회, 프로필 구성, 학사 데이터 초기화를 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final UserService userService;
  private final StudentAcademicRecordRepository studentAcademicRecordRepository;
  private final SemesterAcademicRecordRepository semesterAcademicRecordRepository;
  private final StudentCourseRepository studentCourseRepository;
  private final StudentDesignatedCourseRepository studentDesignatedCourseRepository;
  private final AcademicCache academicCache;

  /**
   * 학생 식별자로 학생을 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 조회된 학생
   * @throws EntityNotFoundException 학생이 없는 경우
   */
  public Student getStudentById(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
  }

  /**
   * 사용자에게 연결된 학생을 반환한다.
   *
   * @param userId 사용자 식별자
   * @return 사용자에게 연결된 학생, 연결되지 않았으면 {@code null}
   * @throws EntityNotFoundException 사용자가 없는 경우
   */
  public Student getStudentByUserId(UUID userId) {
    User user = userService.getUserById(userId);

    return user.getStudent();
  }

  /**
   * 학번으로 학생을 찾는다.
   *
   * @param studentCode 학번
   * @return 학생이 있으면 포함한 선택값
   */
  public Optional<Student> findByStudentCode(String studentCode) {
    return studentRepository.findByStudentCode(studentCode);
  }

  /**
   * 포털 연동이 완료되지 않은 사용자의 학생을 찾는다.
   *
   * @param userId 사용자 식별자
   * @return 연동 대기 학생이 있으면 포함한 선택값
   */
  public Optional<Student> findPortalPendingStudent(UUID userId) {
    return studentRepository.findPortalPendingStudent(userId);
  }

  /**
   * 사용자에게 연결된 학생 행을 쓰기 잠금과 함께 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 잠근 학생이 있으면 포함한 선택값
   */
  @Transactional
  public Optional<Student> findForUpdateByUserId(UUID userId) {
    return studentRepository.findForUpdateByUserId(userId);
  }

  /**
   * 사용자에게 연결된 학생 식별자를 반환한다.
   *
   * @param userId 사용자 식별자
   * @return 연결된 학생 식별자
   * @throws CommonException 사용자에게 학생이 연결되지 않은 경우
   * @throws EntityNotFoundException 연결된 학생의 식별자가 없는 경우
   */
  public UUID getRequiredStudentIdByUserId(UUID userId) {
    Student student = getStudentByUserId(userId);
    if (student == null) {
      throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
    }

    UUID studentId = student.getId();
    if (studentId == null) {
      throw new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND);
    }

    return studentId;
  }

  /**
   * 사용자에 연결된 학생을 포털 재연동 상태로 변경한다.
   *
   * @param user 재연동 상태로 변경할 학생의 사용자
   * @throws EntityNotFoundException 사용자에게 연결된 학생이 없는 경우
   */
  @Transactional
  public void markReconnectedByUser(User user) {
    Student student =
        studentRepository
            .findByUser(user)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
    student.markReconnected();
    studentRepository.save(student);
  }

  /**
   * 학생을 저장한다.
   *
   * @param student 저장할 학생
   */
  @Transactional
  public void save(Student student) {
    studentRepository.save(student);
  }

  /**
   * 학생·사용자·학과·전공 정보를 포함한 프로필을 조회한다.
   *
   * @param studentId 학생 식별자
   * @return 현재 학년·학기와 마지막 동기화 시각을 포함한 학생 프로필
   * @throws CommonException 학생이 없는 경우
   */
  public StudentDto.StudentProfileResponse getStudentProfile(UUID studentId) {
    Student student =
        studentRepository
            .findProfileByIdWithAssociations(studentId)
            .orElseThrow(() -> new CommonException(ErrorCode.STUDENT_NOT_FOUND));

    return buildStudentProfileResponse(student, student.getUser());
  }

  /**
   * 사용자 식별자로 학생 프로필을 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 현재 학년·학기와 마지막 동기화 시각을 포함한 학생 프로필
   * @throws EntityNotFoundException 사용자가 없는 경우
   * @throws CommonException 사용자에게 학생이 연결되지 않은 경우
   */
  public StudentDto.StudentProfileResponse getStudentProfileByUserId(UUID userId) {
    User user =
        userRepository
            .findProfileByIdWithAssociations(userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

    Student student = user.getStudent();
    if (student == null) {
      throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
    }

    return buildStudentProfileResponse(student, user);
  }

  /**
   * 학생의 동기화 학사 데이터를 초기화한다.
   *
   * @param studentId 학생 식별자
   */
  @Transactional
  public void resetBy(UUID studentId) {
    resetInternal(studentId, true);
  }

  /**
   * 포털 재연동 전에 기존 학사 데이터를 초기화한다.
   *
   * @param studentId 초기화할 학생 식별자
   */
  @Transactional
  public void resetForPortalReuse(UUID studentId) {
    resetInternal(studentId, false);
  }

  private void resetInternal(UUID studentId, boolean blockOlderSnapshots) {
    final Optional<Student> lockedStudent = studentRepository.findForUpdateById(studentId);
    studentCourseRepository.deleteByStudentId(studentId);
    semesterAcademicRecordRepository.deleteByStudentId(studentId);
    studentAcademicRecordRepository.deleteByStudentId(studentId);
    studentDesignatedCourseRepository.deleteAllByStudentId(studentId);
    lockedStudent.ifPresent(
        student -> {
          if (blockOlderSnapshots) {
            student.resetDesignatedCourseSnapshot(Instant.now());
          } else {
            student.clearDesignatedCourseSnapshotVersion();
          }
        });
    academicCache.deleteAllByStudentId(studentId);

    log.info("[BIZ] student.reset.done studentId={}", studentId);
  }

  /**
   * 학생의 목표 평점을 갱신한다.
   *
   * @param studentId 학생 식별자
   * @param targetGpa 새 목표 평점
   */
  @Transactional
  public void setStudentTargetGpa(UUID studentId, Double targetGpa) {
    studentRepository.updateTargetGpaByStudentId(studentId, targetGpa);
  }

  private static int getCurrentSemester(Integer gradeLevel, Integer completedSemesters) {

    int safeGradeLevel = (gradeLevel != null) ? gradeLevel : 0;
    int safeCompletedSemesters = (completedSemesters != null) ? completedSemesters : 0;

    int expectedCompleted = (safeGradeLevel - 1) * 2;
    int effectiveCompleted = Math.max(safeCompletedSemesters, expectedCompleted);
    int currentSemester = effectiveCompleted - expectedCompleted + 1;

    if (currentSemester < 1) {
      return 1;
    } else if (currentSemester > 2) {
      return 2;
    }
    return currentSemester;
  }

  private StudentDto.StudentProfileResponse buildStudentProfileResponse(
      Student student, User user) {
    StudentDto.StudentInfoDto studentInfo = StudentDto.StudentInfoDto.from(student);
    int currentSemester =
        getCurrentSemester(studentInfo.gradeLevel(), studentInfo.completedSemesters());
    String lastSyncedAt =
        user != null && user.getLastSyncedAt() != null ? user.getLastSyncedAt().toString() : "";

    return StudentDto.StudentProfileResponse.from(studentInfo, currentSemester, lastSyncedAt);
  }
}
