// dev 테스트 조작에 필요한 선택지와 강의 후보를 조회한다

package com.chukchuk.haksa.domain.testsupport.service;

import com.chukchuk.haksa.domain.course.model.CourseOffering;
import com.chukchuk.haksa.domain.course.model.FacultyDivision;
import com.chukchuk.haksa.domain.course.repository.CourseOfferingRepository;
import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import com.chukchuk.haksa.domain.testsupport.dto.TestDataDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 개발 환경 테스트에 사용할 학과와 개설 강의 선택지를 제공한다. */
@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestOptionService {

  private final DepartmentRepository departmentRepository;
  private final CourseOfferingRepository courseOfferingRepository;

  /**
   * 테스트 데이터 구성에 사용할 기본 선택지를 반환한다.
   *
   * @return 학과와 졸업 영역 등 테스트 구성 선택지
   */
  public TestDataDto.TestOptionsResponse getTestOptions() {
    List<TestDataDto.DepartmentOption> departments =
        departmentRepository.findAll().stream().map(this::toDepartmentOption).toList();
    List<TestDataDto.GraduationAreaOption> areas =
        Arrays.stream(FacultyDivision.values())
            .map(value -> new TestDataDto.GraduationAreaOption(value.name(), value.name()))
            .toList();
    return new TestDataDto.TestOptionsResponse(departments, areas);
  }

  /**
   * 학과 코드 또는 이름에 검색어가 포함된 학과를 반환한다.
   *
   * @param keyword 검색할 과목 코드 또는 이름
   * @return 조건에 일치하는 개발 환경 테스트 데이터 목록
   */
  public List<TestDataDto.DepartmentOption> searchDepartments(String keyword) {
    String normalizedKeyword = normalize(keyword);
    List<Department> departments =
        normalizedKeyword == null
            ? departmentRepository.findAll()
            : departmentRepository.searchAdminDepartments(normalizedKeyword);
    return departments.stream().map(this::toDepartmentOption).toList();
  }

  /**
   * 선택 필터에 맞는 개설 강의 후보를 반환한다.
   *
   * @param request 개설 강의 후보를 제한할 검색 조건
   * @return 조건에 일치하는 개발 환경 테스트 데이터 목록
   */
  public List<TestDataDto.CourseOfferingOption> searchCourseOfferings(
      TestDataDto.CourseOfferingSearchRequest request) {
    String departmentName = resolveDepartmentName(request.departmentId());

    return courseOfferingRepository
        .searchAdminCandidates(
            normalize(request.keyword()),
            request.area(),
            request.year(),
            request.semester(),
            departmentName)
        .stream()
        .map(this::toCourseOfferingOption)
        .toList();
  }

  private TestDataDto.DepartmentOption toDepartmentOption(Department department) {
    return new TestDataDto.DepartmentOption(
        department.getId(),
        department.getDepartmentCode(),
        department.getEstablishedDepartmentName());
  }

  private TestDataDto.CourseOfferingOption toCourseOfferingOption(CourseOffering offering) {
    return new TestDataDto.CourseOfferingOption(
        offering.getId(),
        offering.getCourse().getCourseCode(),
        offering.getCourse().getCourseName(),
        offering.getYear(),
        offering.getSemester(),
        offering.getPoints(),
        offering.getFacultyDivisionName(),
        offering.getRawFacultyDivisionName(),
        resolveDepartmentName(offering));
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
    return departmentRepository
        .findById(departmentId)
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
