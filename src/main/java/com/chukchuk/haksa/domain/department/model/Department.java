package com.chukchuk.haksa.domain.department.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

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

    public Department(String departmentCode, String establishedDepartmentName) {
        this.departmentCode = departmentCode;
        this.establishedDepartmentName = establishedDepartmentName;
    }
}