package com.chukchuk.haksa.domain.admin.auth.repository;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** 관리자 허용 목록 계정을 조회한다. */
public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
  /**
   * Kakao subject와 provider가 일치하는 허용 목록 계정을 조회한다.
   *
   * @param provider OIDC 공급자
   * @param socialId 검증된 Kakao subject
   * @return 일치하는 관리자 계정
   */
  Optional<AdminAccount> findByProviderAndSocialId(String provider, String socialId);
}
