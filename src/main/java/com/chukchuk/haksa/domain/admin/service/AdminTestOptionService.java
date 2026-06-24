// dev 테스트 조작에 필요한 선택지와 강의 후보를 조회한다
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTestOptionService {

    private final DepartmentRepository departmentRepository;
    private final CourseOfferingRepository courseOfferingRepository;

    public AdminTestDto.TestOptionsResponse getTestOptions() {
        List<AdminTestDto.DepartmentOption> departments = departmentRepository.findAll().stream()
                .map(this::toDepartmentOption)
                .toList();
        List<AdminTestDto.GraduationAreaOption> areas = Arrays.stream(FacultyDivision.values())
                .map(value -> new AdminTestDto.GraduationAreaOption(value.name(), value.name()))
                .toList();
        return new AdminTestDto.TestOptionsResponse(departments, areas);
    }

    public List<AdminTestDto.CourseOfferingOption> searchCourseOfferings(AdminTestDto.CourseOfferingSearchRequest request) {
        String departmentName = resolveDepartmentName(request.departmentId());

        return courseOfferingRepository.searchAdminCandidates(
                        normalize(request.keyword()),
                        request.area(),
                        request.year(),
                        request.semester(),
                        departmentName
                ).stream()
                .map(this::toCourseOfferingOption)
                .toList();
    }

    private AdminTestDto.DepartmentOption toDepartmentOption(Department department) {
        return new AdminTestDto.DepartmentOption(
                department.getId(),
                department.getDepartmentCode(),
                department.getEstablishedDepartmentName()
        );
    }

    private AdminTestDto.CourseOfferingOption toCourseOfferingOption(CourseOffering offering) {
        return new AdminTestDto.CourseOfferingOption(
                offering.getId(),
                offering.getCourse().getCourseCode(),
                offering.getCourse().getCourseName(),
                offering.getYear(),
                offering.getSemester(),
                offering.getPoints(),
                offering.getFacultyDivisionName(),
                offering.getRawFacultyDivisionName(),
                resolveDepartmentName(offering)
        );
    }

    private String resolveDepartmentName(CourseOffering offering) {
        if (offering.getDepartment() != null) {
            return offering.getDepartment().getEstablishedDepartmentName();
        }
        return offering.getHostDepartment();
    }

    private String resolveDepartmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .map(Department::getEstablishedDepartmentName)
                .orElseThrow(() -> new CommonException(ErrorCode.INVALID_ARGUMENT));
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
