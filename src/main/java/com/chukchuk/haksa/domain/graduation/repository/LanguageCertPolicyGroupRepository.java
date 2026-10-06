// 외국어 인증 정책 그룹을 조회하고 저장하는 Repository

package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 학과·전공·입학 연도에 적용할 외국어 인증 정책 그룹을 조회한다. */
public interface LanguageCertPolicyGroupRepository
    extends JpaRepository<LanguageCertPolicyGroup, UUID> {
  /**
   * 그룹 키가 일치하는 외국어 인증 정책 그룹을 조회한다.
   *
   * @param groupKey 정책 그룹 키
   * @return 일치하는 정책 그룹이 있으면 포함한 선택값
   */
  Optional<LanguageCertPolicyGroup> findByGroupKey(String groupKey);
}
