package com.chukchuk.haksa.domain.admin.auth.security;

import com.chukchuk.haksa.domain.admin.auth.service.AdminSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 관리자 세션 쿠키를 관리자 principal과 authority로 변환한다. */
@Component
@RequiredArgsConstructor
public class AdminSessionAuthenticationFilter extends OncePerRequestFilter {
  public static final String COOKIE_NAME = "cchaksa_admin_session";
  private final AdminSessionService sessionService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/admin/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    findCookie(request)
        .flatMap(sessionService::authenticate)
        .ifPresent(
            principal -> {
              List<SimpleGrantedAuthority> authorities =
                  List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
              SecurityContextHolder.getContext()
                  .setAuthentication(
                      new UsernamePasswordAuthenticationToken(principal, null, authorities));
            });
    filterChain.doFilter(request, response);
  }

  private java.util.Optional<String> findCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> !value.isBlank())
        .findFirst();
  }
}
