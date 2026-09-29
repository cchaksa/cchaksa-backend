package com.chukchuk.haksa.domain.admin.auth.repository;

import com.chukchuk.haksa.domain.admin.auth.model.AdminLoginChallenge;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 관리자 로그인 challenge를 생성하고 배타적으로 소모한다. */
public interface AdminLoginChallengeRepository extends JpaRepository<AdminLoginChallenge, UUID> {
  /**
   * 중복 소비를 막기 위해 challenge를 배타 잠금으로 조회한다.
   *
   * @param id challenge 식별자
   * @return 잠근 challenge
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from AdminLoginChallenge c where c.id = :id")
  Optional<AdminLoginChallenge> findByIdForUpdate(@Param("id") UUID id);
}
