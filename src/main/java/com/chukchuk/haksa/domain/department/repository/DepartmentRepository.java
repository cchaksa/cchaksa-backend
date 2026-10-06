package com.chukchuk.haksa.domain.department.repository;

import com.chukchuk.haksa.domain.department.model.Department;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 학과 코드·표준 학과명·검색어로 학과를 조회하는 저장소다. */
public interface DepartmentRepository extends JpaRepository<Department, Long> {
  /**
   * 학과 코드가 일치하는 학과를 조회한다.
   *
   * @param departmentCode 학과 코드
   * @return 코드가 일치하는 학과가 있으면 포함한 선택값
   */
  Optional<Department> findByDepartmentCode(String departmentCode); // departmentCode로 학과 찾기

  /**
   * 표준 학과명이 일치하는 학과를 조회한다.
   *
   * @param establishedDepartmentName 표준 학과명
   * @return 표준 학과명이 일치하는 학과 목록
   */
  List<Department> findAllByEstablishedDepartmentName(String establishedDepartmentName);

  /**
   * 코드나 표준 학과명에 검색어가 포함된 학과를 이름순으로 조회한다.
   *
   * @param keyword 검색할 과목 코드 또는 이름
   * @return 코드 또는 이름에 검색어가 포함된 학과 목록
   */
  @Query(
      """
        SELECT d FROM Department d
        WHERE LOWER(d.departmentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(d.establishedDepartmentName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        ORDER BY d.establishedDepartmentName ASC, d.departmentCode ASC
      """)
  List<Department> searchAdminDepartments(String keyword);
}
