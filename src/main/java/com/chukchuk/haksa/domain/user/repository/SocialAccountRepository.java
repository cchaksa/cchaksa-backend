package com.chukchuk.haksa.domain.user.repository;

import com.chukchuk.haksa.domain.user.model.SocialAccount;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndSocialId(OidcProvider provider, String socialId);

    List<SocialAccount> findAllByUserId(UUID userId);
}
