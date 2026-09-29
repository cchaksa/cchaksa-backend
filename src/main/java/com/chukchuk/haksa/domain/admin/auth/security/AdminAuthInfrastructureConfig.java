package com.chukchuk.haksa.domain.admin.auth.security;

import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import java.time.Clock;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.client.RestTemplate;

/** 관리자 인증에서 공유하는 시간과 CSRF 저장소를 구성한다. */
@Configuration
public class AdminAuthInfrastructureConfig {
  /**
   * 관리자 인증의 기준 시계를 제공한다.
   *
   * @return UTC 시스템 시계
   */
  @Bean
  public Clock adminAuthClock() {
    return Clock.systemUTC();
  }

  /**
   * SPA가 읽을 수 있는 CSRF 쿠키 저장소를 제공한다.
   *
   * @param properties 관리자 쿠키 설정
   * @return 관리자 CSRF 저장소
   */
  @Bean
  public CsrfTokenRepository adminCsrfTokenRepository(AdminAuthProperties properties) {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieCustomizer(
        cookie -> cookie.secure(properties.isCookieSecure()).sameSite("Strict"));
    return repository;
  }

  /**
   * Kakao token endpoint 전용 timeout을 적용한 HTTP 클라이언트를 제공한다.
   *
   * @param builder Spring HTTP 클라이언트 빌더
   * @param properties 관리자 Kakao 설정
   * @return 관리자 Kakao token 교환 전용 클라이언트
   */
  @Bean("adminKakaoRestTemplate")
  public RestTemplate adminKakaoRestTemplate(
      RestTemplateBuilder builder, AdminAuthProperties properties) {
    return builder
        .setConnectTimeout(properties.getKakao().getConnectTimeout())
        .setReadTimeout(properties.getKakao().getReadTimeout())
        .build();
  }
}
