// 학과별 졸업요건 row 저장소다
package com.chukchuk.haksa.domain.department.repository;

import com.chukchuk.haksa.domain.department.model.DepartmentAreaRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentAreaRequirementRepository extends JpaRepository<DepartmentAreaRequirement, UUID> {
    boolean existsByDepartmentIdAndAdmissionYearAndAreaType(Long departmentId, Integer admissionYear, String areaType);
}
