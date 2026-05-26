// 학과 코드와 입학년도에 적용되는 외국어 인증 정책 매핑을 조회하는 Repository
package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.DepartmentLanguageCertPolicyMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DepartmentLanguageCertPolicyMappingRepository
        extends JpaRepository<DepartmentLanguageCertPolicyMapping, UUID> {

    @Query("""
            select mapping
            from DepartmentLanguageCertPolicyMapping mapping
            left join fetch mapping.policyGroup
            where mapping.departmentCode = :departmentCode
              and :admissionYear between mapping.admissionYearFrom and mapping.admissionYearTo
            order by mapping.admissionYearFrom desc
            """)
    List<DepartmentLanguageCertPolicyMapping> findApplicableMappings(
            @Param("departmentCode") String departmentCode,
            @Param("admissionYear") Integer admissionYear
    );
}
