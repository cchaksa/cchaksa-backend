// 포털 학생 정보를 학생 초기화 데이터와 응답 요약으로 변환한다.
package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.service.DepartmentService;
import com.chukchuk.haksa.domain.student.model.StudentStatus;
import com.chukchuk.haksa.domain.user.model.StudentInitializationDataType;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult.StudentInfo;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PortalStudentDataMapper {

    private final DepartmentService departmentService;

    PortalStudentData toStudentData(PortalStudentInfo raw) {
        Department department = departmentService.getOrCreateDepartment(
                raw.department().code(),
                raw.department().name()
        );
        Department major = raw.major() != null && raw.major().code() != null
                ? departmentService.getOrCreateDepartment(raw.major().code(), raw.major().name())
                : null;
        Department secondaryMajor = raw.secondaryMajor() != null
                ? departmentService.getOrCreateDepartment(raw.secondaryMajor().code(), raw.secondaryMajor().name())
                : null;

        if (department == null) {
            return null;
        }

        StudentInitializationDataType studentData = StudentInitializationDataType.builder()
                .studentCode(raw.studentCode())
                .name(raw.name())
                .department(department)
                .major(major)
                .secondaryMajor(secondaryMajor)
                .admissionYear(raw.admission().year())
                .semesterEnrolled(raw.admission().semester())
                .isTransferStudent(raw.admission().type().contains("편입"))
                .isGraduated(raw.status().equals(StudentStatus.졸업.name()))
                .status(StudentStatus.valueOf(raw.status()))
                .gradeLevel(raw.academic().gradeLevel())
                .completedSemesters(raw.academic().completedSemesters())
                .admissionType(raw.admission().type())
                .build();

        StudentInfo studentInfo = new StudentInfo(
                raw.name(),
                "수원대학교",
                major != null ? major.getEstablishedDepartmentName() : department.getEstablishedDepartmentName(),
                raw.studentCode(),
                raw.academic().gradeLevel(),
                raw.status(),
                raw.academic().completedSemesters() % 2 == 0 ? 1 : 2
        );

        return new PortalStudentData(studentData, studentInfo);
    }

    record PortalStudentData(
            StudentInitializationDataType studentData,
            StudentInfo studentInfo
    ) {}
}
