// 복수전공 졸업요건 row 저장소다
package com.chukchuk.haksa.domain.department.repository;

import com.chukchuk.haksa.domain.department.model.DualMajorRequirement;
import com.chukchuk.haksa.domain.department.model.MajorRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DualMajorRequirementRepository extends JpaRepository<DualMajorRequirement, UUID> {
    boolean existsByDepartmentIdAndAdmissionYearAndMajorRoleAndAreaType(
            Long departmentId,
            Integer admissionYear,
            MajorRole majorRole,
            String areaType
    );

    List<DualMajorRequirement> findAllByDepartmentIdAndAdmissionYearAndMajorRole(
            Long departmentId,
            Integer admissionYear,
            MajorRole majorRole
    );
}
