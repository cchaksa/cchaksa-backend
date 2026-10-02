package com.chukchuk.haksa.domain.admin.auth.repository;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 관리자 로컬 계정을 조회하고 비밀번호 변경을 직렬화한다. */
public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {
  /**
   * 정규화된 login ID가 일치하는 계정을 조회한다.
   *
   * @param loginId 대소문자를 구분하는 원문 login ID
   * @return 일치하는 관리자 계정
   */
  Optional<AdminAccount> findByLoginId(String loginId);

  /**
   * 비밀번호 변경 중 계정 행을 잠가 동시 변경을 직렬화한다.
   *
   * @param id 관리자 계정 UUID
   * @return write lock을 획득한 관리자 계정
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AdminAccount a where a.id = :id")
  Optional<AdminAccount> findByIdForUpdate(@Param("id") UUID id);
}
