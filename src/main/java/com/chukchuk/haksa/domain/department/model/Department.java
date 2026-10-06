package com.chukchuk.haksa.domain.department.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 학과 도메인 상태를 표현한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "departments")
public class Department extends BaseEntity {

  @Id
  @GeneratedValue(strategy = IDENTITY)
  private Long id;

  @Column(name = "department_code", unique = true)
  private String departmentCode;

  @Column(name = "established_department_name")
  private String establishedDepartmentName;

  /**
   * 학과 코드와 표준 학과명으로 학과를 생성한다.
   *
   * @param departmentCode 학과 코드
   * @param establishedDepartmentName 표준 학과명
   */
  public Department(String departmentCode, String establishedDepartmentName) {
    this.departmentCode = departmentCode;
    this.establishedDepartmentName = establishedDepartmentName;
  }
}
