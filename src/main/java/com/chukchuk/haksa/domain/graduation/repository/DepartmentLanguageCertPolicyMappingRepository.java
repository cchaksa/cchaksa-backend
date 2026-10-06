// 학과 코드와 입학년도에 적용되는 외국어 인증 정책 매핑을 조회하는 Repository

package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.DepartmentLanguageCertPolicyMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 학과 코드와 입학 연도에 적용되는 외국어 인증 정책 매핑을 조회하는 저장소다. */
public interface DepartmentLanguageCertPolicyMappingRepository
    extends JpaRepository<DepartmentLanguageCertPolicyMapping, UUID> {

  /**
   * 학과와 입학 연도에 적용되는 외국어 인증 정책 매핑을 조회한다.
   *
   * @param departmentCode 학과 코드
   * @param admissionYear 입학 연도
   * @return 적용 시작 연도가 최신인 순서의 정책 매핑 목록
   */
  @Query(
      """
            select mapping
            from DepartmentLanguageCertPolicyMapping mapping
            left join fetch mapping.policyGroup
            where mapping.departmentCode = :departmentCode
              and :admissionYear between mapping.admissionYearFrom and mapping.admissionYearTo
            order by mapping.admissionYearFrom desc
      """)
  List<DepartmentLanguageCertPolicyMapping> findApplicableMappings(
      @Param("departmentCode") String departmentCode,
      @Param("admissionYear") Integer admissionYear);
}
