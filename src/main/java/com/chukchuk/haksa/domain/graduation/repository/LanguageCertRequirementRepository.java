// 외국어 인증 정책 그룹별 시험 기준을 조회하는 Repository
package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import com.chukchuk.haksa.domain.graduation.model.LanguageCertRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LanguageCertRequirementRepository extends JpaRepository<LanguageCertRequirement, UUID> {
    List<LanguageCertRequirement> findAllByPolicyGroupOrderBySortOrderAsc(LanguageCertPolicyGroup policyGroup);
}
