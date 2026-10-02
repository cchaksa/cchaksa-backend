package com.chukchuk.haksa.domain.testsupport.config;

import com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilter;
import com.chukchuk.haksa.global.security.handler.CustomAccessDeniedHandler;
import com.chukchuk.haksa.global.security.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/** 개발·테스트 환경에서만 테스트 데이터 API의 공개·인증 경계를 구성한다. */
@Configuration
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class TestSupportSecurityConfig {
  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/test/users",
    "/api/test/options",
    "/api/test/departments",
    "/api/test/course-offerings",
    "/api/test/lecture-evaluations/**"
  };

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;

  /**
   * 테스트 데이터 API에 환경 한정 공개 규칙과 USER 인증을 적용한다.
   *
   * @param http 테스트 지원 보안 builder
   * @param corsConfigurationSource 현재 환경의 CORS 정책
   * @return 테스트 지원 전용 필터 체인
   * @throws Exception 필터 체인을 구성할 수 없는 경우
   */
  @Bean
  @Order(2)
  public SecurityFilterChain testSupportSecurityFilterChain(
      HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
    return http.securityMatcher("/api/test/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/api/test/**")
                    .permitAll()
                    .requestMatchers(PUBLIC_ENDPOINTS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
