package com.chukchuk.haksa.domain.user.repository;

import com.chukchuk.haksa.domain.user.model.SocialAccount;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 소셜 제공자 식별정보 또는 사용자로 소셜 계정을 조회·삭제하는 저장소다. */
@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
  /**
   * 소셜 로그인 제공자와 제공자 사용자 식별자가 일치하는 계정을 찾는다.
   *
   * @param provider 소셜 로그인 제공자
   * @param socialId 제공자가 발급한 사용자 식별자
   * @return 제공자 계정이 일치하면 포함한 선택값
   */
  Optional<SocialAccount> findByProviderAndSocialId(OidcProvider provider, String socialId);

  /**
   * 사용자에 연결된 모든 소셜 계정을 조회한다.
   *
   * @param userId 사용자 식별자
   * @return 사용자에 연결된 소셜 계정 목록
   */
  List<SocialAccount> findAllByUserId(UUID userId);

  /**
   * 지정된 데이터를 삭제한다.
   *
   * @param user 연결할 사용자
   */
  void deleteByUser(User user);
}
