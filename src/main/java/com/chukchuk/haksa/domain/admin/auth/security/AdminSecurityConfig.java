package com.chukchuk.haksa.domain.admin.auth.security;

import com.chukchuk.haksa.global.security.filter.JwtAuthenticationFilter;
import com.chukchuk.haksa.global.security.handler.CustomAccessDeniedHandler;
import com.chukchuk.haksa.global.security.handler.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** 쿠키 세션과 CSRF를 사용하는 관리자 API 전용 보안 체인이다. */
@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {
  private final AdminSessionAuthenticationFilter adminSessionAuthenticationFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;
  private final CsrfTokenRepository adminCsrfTokenRepository;

  /**
   * 관리자 URL에 쿠키 세션, CSRF와 역할 검사를 적용한다.
   *
   * @param http 관리자 보안 builder
   * @return 관리자 전용 보안 필터 체인
   * @throws Exception 필터 체인을 구성할 수 없는 경우
   */
  @Bean
  @Order(1)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName("_csrf");
    return http.securityMatcher("/api/admin/**")
        .cors(cors -> {})
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(adminCsrfTokenRepository)
                    .csrfTokenRequestHandler(requestHandler))
        .securityContext(
            context ->
                context
                    .securityContextRepository(new RequestAttributeSecurityContextRepository())
                    .requireExplicitSave(true))
        .requestCache(cache -> cache.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/admin/auth/csrf", "/api/admin/auth/signin")
                    .permitAll()
                    .anyRequest()
                    .hasAnyRole("ADMIN", "CS_AGENT"))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(adminSessionAuthenticationFilter, JwtAuthenticationFilter.class)
        .build();
  }
}
