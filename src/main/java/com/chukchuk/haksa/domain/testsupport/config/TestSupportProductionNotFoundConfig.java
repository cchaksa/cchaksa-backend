package com.chukchuk.haksa.domain.testsupport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** 운영에서 존재하지 않는 테스트 지원 경로가 인증 응답 대신 MVC 404에 도달하게 한다. */
@Configuration
@Profile("prod")
public class TestSupportProductionNotFoundConfig {
  /**
   * 운영 테스트 경로를 핸들러 없는 MVC 요청으로 전달한다.
   *
   * @param http 운영 no-handler 보안 builder
   * @return 테스트 경로 전용 필터 체인
   * @throws Exception 필터 체인을 구성할 수 없는 경우
   */
  @Bean
  @Order(2)
  public SecurityFilterChain testSupportProductionNotFoundFilterChain(HttpSecurity http)
      throws Exception {
    return http.securityMatcher("/api/test/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
  }
}
