// 외국어 인증 정책 그룹별 시험 기준을 조회하는 Repository

package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertRequirement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 입학 연도와 인증 정책에 맞는 외국어 졸업 인증 요건을 조회한다. */
public interface LanguageCertRequirementRepository
    extends JpaRepository<LanguageCertRequirement, UUID> {
  /**
   * 정책 그룹의 외국어 인증 요건을 표시 순서대로 조회한다.
   *
   * @param policyGroup 정책 그룹
   * @return 표시 순서로 정렬된 외국어 인증 요건 목록
   */
  List<LanguageCertRequirement> findAllByPolicyGroupOrderBySortOrderAsc(
      LanguageCertPolicyGroup policyGroup);
}
