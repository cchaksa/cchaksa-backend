package com.chukchuk.haksa.domain.auth.repository;

import com.chukchuk.haksa.domain.auth.entity.RefreshToken;
import java.util.Date;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 세션별 리프레시 토큰을 저장하고 만료 시각·사용자 기준 삭제 연산을 제공한다. */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
  /**
   * 기준 시각 전에 만료된 리프레시 토큰을 삭제한다.
   *
   * @param now 처리 기준 시각
   * @return 기준 시각 이전에 만료되어 삭제된 리프레시 토큰 수
   */
  int deleteByExpiryBefore(Date now);

  /**
   * 사용자의 모든 refresh token을 삭제한다.
   *
   * @param userId 사용자 식별자
   * @return 삭제된 token 수
   */
  int deleteByUserId(String userId);
}
