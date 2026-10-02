package com.chukchuk.haksa.domain.admin.auth.repository;

import com.chukchuk.haksa.domain.admin.auth.model.AdminSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 관리자 세션 해시 조회와 일괄 폐기 연산을 제공한다. */
public interface AdminSessionRepository extends JpaRepository<AdminSession, UUID> {
  /**
   * 세션 해시와 관리자 계정을 함께 조회한다.
   *
   * @param tokenHash 세션 원문의 SHA-256 해시
   * @return 관리자 계정을 포함한 세션
   */
  @Query("select s from AdminSession s join fetch s.adminAccount where s.tokenHash = :tokenHash")
  Optional<AdminSession> findByTokenHash(@Param("tokenHash") String tokenHash);

  /**
   * 한 관리자에게 발급한 모든 활성 세션을 폐기한다.
   *
   * @param adminAccountId 관리자 계정 식별자
   * @param now 폐기 시각
   * @return 폐기된 세션 수
   */
  @Modifying
  @Query(
      "update AdminSession s set s.revokedAt = :now "
          + "where s.adminAccount.id = :adminAccountId and s.revokedAt is null")
  int revokeAllByAdminAccountId(
      @Param("adminAccountId") UUID adminAccountId, @Param("now") Instant now);
}
