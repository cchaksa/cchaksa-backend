package com.chukchuk.haksa.domain.user.repository;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class UserPortalConnectionRepository {
    private final UserService userService;
    private final StudentService studentService;

    @Transactional
    public void initializePortalConnection(User user, StudentInitializationDataType studentData) {
        Student existingStudent = studentService.findPortalPendingStudent(user.getId()).orElse(user.getStudent());
        if (existingStudent != null) {
            reuseExistingStudent(existingStudent, user, studentData);
            return;
        }

        Department department = studentData.getDepartment();
        Department major = studentData.getMajor();
        Department secondaryMajor = studentData.getSecondaryMajor();

        Student student = Student.builder()
                .studentCode(studentData.getStudentCode())
                .name(studentData.getName())
                .department(department)
                .major(major)
                .secondaryMajor(secondaryMajor)
                .admissionYear(studentData.getAdmissionYear())
                .semesterEnrolled(studentData.getSemesterEnrolled())
                .isTransferStudent(studentData.isTransferStudent())
                .isGraduated(studentData.isGraduated())
                .status(studentData.getStatus())
                .gradeLevel(studentData.getGradeLevel())
                .completedSemesters(studentData.getCompletedSemesters())
                .admissionType(studentData.getAdmissionType())
                .user(user)
                .build();

        user.setStudent(student);
        userService.save(user);
        studentService.save(student);
        userService.evictUserDetailsCache(user.getId());
    }

    private void reuseExistingStudent(Student student, User user, StudentInitializationDataType studentData) {
        studentService.resetBy(student.getId());
        student.clearAcademicAssociations();
        student.updateInfo(
                studentData.getName(),
                studentData.getDepartment(),
                studentData.getMajor(),
                studentData.getSecondaryMajor(),
                studentData.getAdmissionYear(),
                studentData.getSemesterEnrolled(),
                studentData.isTransferStudent(),
                studentData.isGraduated(),
                studentData.getStatus(),
                studentData.getGradeLevel(),
                studentData.getCompletedSemesters(),
                studentData.getAdmissionType()
        );
        student.updateUser(user);
        user.setStudent(student);

        studentService.save(student);
        userService.save(user);
        userService.evictUserDetailsCache(user.getId());
    }

    @Transactional
    public void refreshPortalConnection(User user, StudentInitializationDataType studentData) {
        // 학생 정보 조회
        Student student = studentService.getStudentByUserId(user.getId());

        // 학과 및 전공 정보
        Department department = studentData.getDepartment();
        Department major = studentData.getMajor();
        Department secondaryMajor = studentData.getSecondaryMajor();

        if (student.needsUpdate(studentData)) {
            // 기존 학생 정보 갱신
            student.updateInfo(
                    studentData.getName(),
                    department,
                    major,
                    secondaryMajor,
                    studentData.getAdmissionYear(),
                    studentData.getSemesterEnrolled(),
                    studentData.isTransferStudent(),
                    studentData.isGraduated(),
                    studentData.getStatus(),
                    studentData.getGradeLevel(),
                    studentData.getCompletedSemesters(),
                    studentData.getAdmissionType()
            );
            studentService.save(student);
        }

        // 마지막 동기화 시간 갱신
        user.updateLastSyncedAt(Instant.now());
        userService.save(user);
    }
}
