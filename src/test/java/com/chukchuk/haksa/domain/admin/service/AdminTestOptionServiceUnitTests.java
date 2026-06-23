// dev 테스트 옵션 조회 서비스 동작을 검증하는 테스트
package com.chukchuk.haksa.domain.admin.service;

import com.chukchuk.haksa.domain.admin.dto.AdminTestDto;
import com.chukchuk.haksa.domain.course.model.Course;
import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTestOptionServiceUnitTests {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @InjectMocks
    private AdminTestOptionService optionService;

    @Test
    @DisplayName("테스트 옵션 조회 시 학과와 졸업요건 영역을 반환한다")
    void getTestOptions_returnsDepartmentsAndAreas() {
        when(departmentRepository.findAll()).thenReturn(List.of(new Department("CSE", "컴퓨터학과")));

        AdminTestDto.TestOptionsResponse response = optionService.getTestOptions();

        assertThat(response.departments()).hasSize(1);
        assertThat(response.departments().get(0).name()).isEqualTo("컴퓨터학과");
        assertThat(response.graduationAreas())
                .extracting(AdminTestDto.GraduationAreaOption::code)
                .contains("전핵", "전선", "복핵", "복선");
    }

    @Test
    @DisplayName("강의 후보 검색 시 개설강의 정보를 프론트 선택지로 변환한다")
    void searchCourseOfferings_returnsCourseOptions() {
        CourseOffering offering = mock(CourseOffering.class);
        Course course = new Course("CSE101", "자료구조");
        Department department = new Department("CSE", "컴퓨터학과");
        when(offering.getId()).thenReturn(10L);
        when(offering.getCourse()).thenReturn(course);
        when(offering.getYear()).thenReturn(2024);
        when(offering.getSemester()).thenReturn(10);
        when(offering.getPoints()).thenReturn(3);
        when(offering.getFacultyDivisionName()).thenReturn(FacultyDivision.전핵);
        when(offering.getRawFacultyDivisionName()).thenReturn(null);
        when(offering.getDepartment()).thenReturn(department);
        when(courseOfferingRepository.searchAdminCandidates("자료", FacultyDivision.전핵, 2024, 10, 1L))
                .thenReturn(List.of(offering));

        List<AdminTestDto.CourseOfferingOption> response = optionService.searchCourseOfferings(
                new AdminTestDto.CourseOfferingSearchRequest("자료", FacultyDivision.전핵, 2024, 10, 1L)
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).offeringId()).isEqualTo(10L);
        assertThat(response.get(0).courseCode()).isEqualTo("CSE101");
        assertThat(response.get(0).courseName()).isEqualTo("자료구조");
        assertThat(response.get(0).area()).isEqualTo(FacultyDivision.전핵);
        assertThat(response.get(0).departmentName()).isEqualTo("컴퓨터학과");
    }
}
