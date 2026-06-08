// 외국어 인증 정책 그룹을 조회하고 저장하는 Repository
package com.chukchuk.haksa.domain.graduation.repository;

import com.chukchuk.haksa.domain.graduation.model.LanguageCertPolicyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LanguageCertPolicyGroupRepository extends JpaRepository<LanguageCertPolicyGroup, UUID> {
    Optional<LanguageCertPolicyGroup> findByGroupKey(String groupKey);
}
