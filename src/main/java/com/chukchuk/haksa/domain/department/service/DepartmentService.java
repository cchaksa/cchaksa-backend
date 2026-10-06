package com.chukchuk.haksa.domain.department.service;

import com.chukchuk.haksa.domain.department.model.Department;
import com.chukchuk.haksa.domain.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 학과 코드가 같은 학과를 재사용하고 없으면 표준 학과명으로 생성한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {
  private final DepartmentRepository departmentRepository;

  // 학과 코드로 조회하고, 없으면 새로 생성
  /**
   * 학과 코드가 같은 학과를 재사용하고, 없으면 새로 저장한다.
   *
   * @param departmentCode 학과 코드
   * @param establishedDepartmentName established 학과 이름
   * @return 학과 코드가 같으면 기존 학과, 없으면 새로 저장한 학과
   */
  @Transactional
  public Department getOrCreateDepartment(String departmentCode, String establishedDepartmentName) {
    return departmentRepository
        .findByDepartmentCode(departmentCode)
        .orElseGet(
            () -> {
              Department department = new Department(departmentCode, establishedDepartmentName);
              return departmentRepository.save(department);
            });
  }
}
