// dev 테스트 데이터 수정을 현재 인증 계정 범위로 처리한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.academic.record.model.StudentCourse;
import com.chukchuk.haksa.domain.academic.record.repository.StudentCourseRepository;
import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.EvaluationType;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.repository.CourseRepository;
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

import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTestMutationService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
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
            if (isSameDepartment(major, secondaryMajor)) {
                throw new CommonException(ErrorCode.INVALID_ARGUMENT);
            }
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

    public AdminTestDto.TestCourseResponse createTestCourse(UUID userId, AdminTestDto.CreateTestCourseRequest request) {
        if (request == null || request.area() == null) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        Student student = getRequiredStudent(userId);
        Department department = request.departmentId() != null ? getDepartment(request.departmentId()) : null;
        String courseCode = resolveTestCourseCode(request.courseCode());
        String courseName = normalizeOrDefault(request.courseName(), "테스트 강의");
        Integer year = request.year() != null ? request.year() : Year.now(SEOUL).getValue();
        Integer semester = request.semester() != null ? request.semester() : 10;
        Integer credits = request.credits() != null ? request.credits() : 3;
        String hostDepartment = department != null
                ? department.getEstablishedDepartmentName()
                : normalize(request.hostDepartment());

        Course course = courseRepository.save(new Course(courseCode, courseName));
        CourseOffering offering = courseOfferingRepository.save(new CourseOffering(
                subjectEstablishmentSemester(year, semester),
                false,
                year,
                semester,
                hostDepartment,
                "test",
                null,
                null,
                credits,
                EvaluationType.UNKNOWN,
                request.area(),
                course,
                null,
                department,
                null
        ));
        StudentCourse studentCourse = studentCourseRepository.save(new StudentCourse(
                student,
                offering,
                new Grade(GradeType.from(request.grade())),
                credits,
                Boolean.TRUE.equals(request.isRetake()),
                request.originalScore(),
                false
        ));

        academicCache.deleteAllByStudentId(student.getId());
        return new AdminTestDto.TestCourseResponse(
                studentCourse.getId(),
                offering.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                offering.getFacultyDivisionName()
        );
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

    private boolean isSameDepartment(Department left, Department right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return Objects.equals(left.getId(), right.getId());
        }
        return left.getDepartmentCode() != null
                && Objects.equals(left.getDepartmentCode(), right.getDepartmentCode());
    }

    private String resolveTestCourseCode(String courseCode) {
        String normalized = normalize(courseCode);
        if (normalized == null) {
            return "test_" + UUID.randomUUID().toString().substring(0, 8);
        }
        if (normalized.startsWith("test_")) {
            return normalized;
        }
        return "test_" + normalized;
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized != null ? normalized : defaultValue;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Integer subjectEstablishmentSemester(Integer year, Integer semester) {
        int semesterOrder = switch (semester) {
            case 20 -> 2;
            case 10 -> 1;
            case 15 -> 3;
            case 25 -> 4;
            default -> semester;
        };
        return year * 10 + semesterOrder;
    }
}
