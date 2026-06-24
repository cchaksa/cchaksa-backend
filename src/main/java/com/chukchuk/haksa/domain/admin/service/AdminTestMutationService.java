// dev 테스트 데이터 수정을 현재 인증 계정 범위로 처리한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.student.model.Grade;
import com.chukchuk.haksa.domain.student.model.GradeType;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.repository.StudentRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTestMutationService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentCourseRepository studentCourseRepository;
    private final AcademicCache academicCache;

    public void updateGraduationCourses(UUID userId, AdminTestDto.UpdateGraduationCoursesRequest request) {
        Student student = getRequiredStudent(userId);

        List<Long> addOfferingIds = nonNullList(request.addOfferingIds());
        if (!addOfferingIds.isEmpty()) {
            for (CourseOffering offering : courseOfferingRepository.findAllById(addOfferingIds)) {
                validateArea(request, offering);
                StudentCourse studentCourse = new StudentCourse(
                        student,
                        offering,
                        new Grade(GradeType.from(request.grade())),
                        request.points() != null ? request.points() : offering.getPoints(),
                        Boolean.TRUE.equals(request.isRetake()),
                        request.originalScore(),
                        false
                );
                studentCourseRepository.save(studentCourse);
            }
        }

        List<Long> removeStudentCourseIds = nonNullList(request.removeStudentCourseIds());
        if (!removeStudentCourseIds.isEmpty()) {
            studentCourseRepository.deleteOwnedByStudentIdAndIdIn(student.getId(), removeStudentCourseIds);
        }

        academicCache.deleteAllByStudentId(student.getId());
    }

    public void updateMajor(UUID userId, AdminTestDto.UpdateMajorRequest request) {
        Student student = getRequiredStudent(userId);
        Department major = request.majorDepartmentId() != null
                ? getDepartment(request.majorDepartmentId())
                : student.getMajor();
        Department secondaryMajor = null;

        if (request.dualMajorEnabled()) {
            if (request.secondaryMajorDepartmentId() == null) {
                throw new CommonException(ErrorCode.INVALID_ARGUMENT);
            }
            secondaryMajor = getDepartment(request.secondaryMajorDepartmentId());
        }

        student.updateMajors(major, secondaryMajor);
        studentRepository.save(student);
        academicCache.deleteAllByStudentId(student.getId());
    }

    public void resetCurrentAccount(UUID userId) {
        Student student = getRequiredStudent(userId);

        studentCourseRepository.deleteByStudentId(student.getId());
        student.updateMajors(student.getDepartment(), null);
        studentRepository.save(student);
        academicCache.deleteAllByStudentId(student.getId());
    }

    private Student getRequiredStudent(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        Student student = user.getStudent();
        if (student == null) {
            throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
        }
        return student;
    }

    private Department getDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new CommonException(ErrorCode.INVALID_ARGUMENT));
    }

    private void validateArea(AdminTestDto.UpdateGraduationCoursesRequest request, CourseOffering offering) {
        if (request.area() == null || offering.getFacultyDivisionName() == null) {
            return;
        }
        if (request.area() != offering.getFacultyDivisionName()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private List<Long> nonNullList(List<Long> values) {
        return values != null ? values : List.of();
    }
}
