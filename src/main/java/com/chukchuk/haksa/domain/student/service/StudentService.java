package com.chukchuk.haksa.domain.student.service;

import com.chukchuk.haksa.domain.student.dto.StudentDto;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserService userService;

    public Student getStudentById(UUID studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
    }

    public Student getStudentByUserId(UUID userId) {
        User user = userService.getUserById(userId);

        return user.getStudent();
    }

    @Transactional
    public void markReconnectedByUser(User user) {
        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
        student.markReconnected();
        studentRepository.save(student);
    }

    @Transactional
    public void save(Student student) {
        studentRepository.save(student);
    }

    public StudentDto.StudentProfileResponse getStudentProfile(UUID studentId) {
        Student student = getStudentById(studentId);

        StudentDto.StudentInfoDto studentInfo = StudentDto.StudentInfoDto.from(student);
        int currentSemester = getCurrentSemester(studentInfo.gradeLevel(), studentInfo.completedSemesters());

        User user = student.getUser();
        String lastSyncedAt = user.getLastSyncedAt() != null ? user.getLastSyncedAt().toString() : "";

        return StudentDto.StudentProfileResponse.from(studentInfo, currentSemester, lastSyncedAt);
    }

    @Transactional
    public void resetBy(UUID studentId) {
        Student student = getStudentById(studentId);
        student.resetAcademicData();

        log.info("[BIZ] student.reset.done userIdHash={}", HashUtil.sha256Short(studentId.toString()));
    }

    @Transactional
    public void setStudentTargetGpa(UUID studentId, Double targetGpa) {
        // 유효성 실패는 GlobalExceptionHandler가 WARN 처리
        Student student = getStudentById(studentId);
        if (targetGpa != null && (targetGpa < 0 || targetGpa > 4.5)) {
            log.warn("[BIZ] student.target_gpa.set.invalid userIdHash={} value={}",
                    HashUtil.sha256Short(studentId.toString()), targetGpa);
            throw new CommonException(ErrorCode.INVALID_TARGET_GPA);
        }
        student.setTargetGpa(targetGpa);
        studentRepository.save(student);
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
}
