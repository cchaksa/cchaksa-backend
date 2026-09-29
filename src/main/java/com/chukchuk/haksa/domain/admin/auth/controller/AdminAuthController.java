package com.chukchuk.haksa.domain.admin.auth.controller;

import com.chukchuk.haksa.domain.admin.auth.dto.AdminAuthDto;
import com.chukchuk.haksa.domain.admin.auth.model.AdminLoginChallenge;
import com.chukchuk.haksa.domain.admin.auth.security.AdminPrincipal;
import com.chukchuk.haksa.domain.admin.auth.security.AdminSessionAuthenticationFilter;
import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthService;
import com.chukchuk.haksa.domain.admin.auth.service.AdminChallengeService;
import com.chukchuk.haksa.domain.admin.auth.service.AdminSessionService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 challenge, 로그인, 현재 계정, 로그아웃 API를 제공한다. */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
  private final AdminChallengeService challengeService;
  private final AdminAuthService authService;
  private final AdminSessionService sessionService;
  private final AdminAuthProperties properties;
  private final CsrfTokenRepository adminCsrfTokenRepository;

  /**
   * 로그인 nonce와 CSRF 쿠키를 발급한다.
   *
   * @param csrfToken 지연 생성할 CSRF token
   * @return 로그인 challenge
   */
  @Operation(summary = "관리자 로그인 challenge 발급")
  @GetMapping("/challenge")
  public SuccessResponse<AdminAuthDto.ChallengeResponse> challenge(CsrfToken csrfToken) {
    csrfToken.getToken();
    AdminLoginChallenge challenge = challengeService.issue();
    return SuccessResponse.of(
        new AdminAuthDto.ChallengeResponse(challenge.getId(), challenge.getNonce()));
  }

  /**
   * 검증된 Kakao 계정에 관리자 세션을 발급한다.
   *
   * @param request challenge와 ID Token
   * @return 관리자 계정과 세션 쿠키
   */
  @Operation(summary = "카카오 ID Token으로 관리자 로그인")
  @Parameter(
      name = "X-XSRF-TOKEN",
      in = ParameterIn.HEADER,
      required = true,
      description = "challenge 응답에서 발급된 CSRF 쿠키 값")
  @PostMapping("/signin")
  public ResponseEntity<SuccessResponse<AdminAuthDto.AdminResponse>> signIn(
      @Valid @RequestBody AdminAuthDto.SignInRequest request) {
    AdminAuthService.SignInResult result =
        authService.signIn(request.challengeId(), request.idToken());
    AdminAuthDto.AdminResponse body =
        new AdminAuthDto.AdminResponse(
            result.account().getId(),
            result.account().getDisplayName(),
            result.account().getAdminRole());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessionCookie(result.session()).toString())
        .body(SuccessResponse.of(body));
  }

  /**
   * 현재 세션의 관리자 정보를 반환한다.
   *
   * @param principal 인증된 관리자
   * @return 관리자 계정 정보
   */
  @Operation(summary = "현재 관리자 계정 조회")
  @SecurityRequirement(name = "adminSession")
  @GetMapping("/me")
  public SuccessResponse<AdminAuthDto.AdminResponse> me(
      @AuthenticationPrincipal AdminPrincipal principal) {
    return SuccessResponse.of(
        new AdminAuthDto.AdminResponse(
            principal.adminAccountId(), principal.displayName(), principal.role()));
  }

  /**
   * 현재 관리자 세션을 폐기하고 쿠키를 만료한다.
   *
   * @param request 현재 요청
   * @param response CSRF 쿠키를 만료할 응답
   * @param csrfToken 검증된 CSRF token
   * @return 204 응답
   */
  @Operation(summary = "현재 관리자 세션 로그아웃")
  @SecurityRequirement(name = "adminSession")
  @Parameter(
      name = "X-XSRF-TOKEN",
      in = ParameterIn.HEADER,
      required = true,
      description = "관리자 CSRF 쿠키 값")
  @PostMapping("/signout")
  public ResponseEntity<Void> signOut(
      HttpServletRequest request, HttpServletResponse response, CsrfToken csrfToken) {
    findSessionCookie(request).ifPresent(sessionService::revoke);
    adminCsrfTokenRepository.saveToken(null, request, response);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString())
        .build();
  }

  private ResponseCookie sessionCookie(AdminSessionService.CreatedSession session) {
    return ResponseCookie.from(AdminSessionAuthenticationFilter.COOKIE_NAME, session.token())
        .httpOnly(true)
        .secure(properties.isCookieSecure())
        .sameSite("Strict")
        .path("/api/admin")
        .maxAge(properties.getAbsoluteTimeout())
        .build();
  }

  private ResponseCookie expiredSessionCookie() {
    return ResponseCookie.from(AdminSessionAuthenticationFilter.COOKIE_NAME, "")
        .httpOnly(true)
        .secure(properties.isCookieSecure())
        .sameSite("Strict")
        .path("/api/admin")
        .maxAge(Duration.ZERO)
        .build();
  }

  private java.util.Optional<String> findSessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(cookie -> AdminSessionAuthenticationFilter.COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }
}
