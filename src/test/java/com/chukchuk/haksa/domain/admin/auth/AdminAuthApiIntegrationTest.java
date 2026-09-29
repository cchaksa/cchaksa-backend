package com.chukchuk.haksa.domain.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import com.chukchuk.haksa.domain.admin.auth.model.AdminStatus;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminAccountRepository;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminLoginChallengeRepository;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminSessionRepository;
import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import com.chukchuk.haksa.domain.admin.auth.service.AdminKakaoOidcService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AdminAccountRepository accountRepository;
  @Autowired private AdminLoginChallengeRepository challengeRepository;
  @Autowired private AdminSessionRepository sessionRepository;
  @Autowired private AdminAuthProperties authProperties;
  @MockBean private AdminKakaoOidcService oidcService;

  @BeforeEach
  void clean() {
    sessionRepository.deleteAll();
    challengeRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void challengeLoginMeAndSignoutUseCsrfProtectedAdminSession() throws Exception {
    accountRepository.save(
        new AdminAccount(
            UUID.randomUUID(),
            "kakao-cs-1",
            "CS 담당자",
            AdminRole.CS_AGENT,
            AdminStatus.ACTIVE,
            "bootstrap"));
    when(oidcService.exchangeAndVerify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("kakao-cs-1"));

    Challenge challenge = issueChallenge();
    MvcResult signIn =
        mockMvc
            .perform(
                post("/api/admin/auth/signin")
                    .cookie(challenge.csrfCookie(), challenge.loginCookie())
                    .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(signInBody(challenge, challenge.state())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.adminRole").value("CS_AGENT"))
            .andExpect(jsonPath("$.data.role").doesNotExist())
            .andReturn();

    Cookie session = signIn.getResponse().getCookie("cchaksa_admin_session");
    mockMvc
        .perform(get("/api/admin/auth/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("CS 담당자"));

    mockMvc
        .perform(post("/api/admin/auth/signout").cookie(session))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/api/admin/auth/signout")
                .cookie(session, challenge.csrfCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue()))
        .andExpect(status().isNoContent());
    mockMvc.perform(get("/api/admin/auth/me").cookie(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void unregisteredAndInactiveAccountsAreDeniedWithoutAccountDisclosure() throws Exception {
    when(oidcService.exchangeAndVerify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("not-registered"));
    Challenge challenge = issueChallenge();

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie(), challenge.loginCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(challenge, challenge.state())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("A09"));

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie(), challenge.loginCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(challenge, challenge.state())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("A10"));

    accountRepository.save(
        new AdminAccount(
            UUID.randomUUID(),
            "inactive-admin",
            "비활성 관리자",
            AdminRole.ADMIN,
            AdminStatus.INACTIVE,
            "bootstrap"));
    when(oidcService.exchangeAndVerify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("inactive-admin"));
    Challenge inactiveChallenge = issueChallenge();
    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(inactiveChallenge.csrfCookie(), inactiveChallenge.loginCookie())
                .header("X-XSRF-TOKEN", inactiveChallenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(inactiveChallenge, inactiveChallenge.state())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("A09"));
  }

  @Test
  void unauthenticatedIsUnauthorizedAndRegularUserIsForbidden() throws Exception {
    mockMvc.perform(get("/api/admin/auth/me")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/admin/auth/me").with(user("regular-user")))
        .andExpect(status().isForbidden());
  }

  @Test
  void stateMustMatchBeforeAuthorizationCodeIsExchanged() throws Exception {
    accountRepository.save(
        new AdminAccount(
            UUID.randomUUID(),
            "state-admin",
            "상태 검증 관리자",
            AdminRole.ADMIN,
            AdminStatus.ACTIVE,
            "bootstrap"));
    when(oidcService.exchangeAndVerify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("state-admin"));
    Challenge challenge = issueChallenge();

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie(), challenge.loginCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(challenge, "wrong-state")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("A10"));
    verifyNoInteractions(oidcService);

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie(), challenge.loginCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(challenge, challenge.state())))
        .andExpect(status().isOk());
  }

  @Test
  void challengeIsBoundToLoginStartBrowserCookie() throws Exception {
    accountRepository.save(
        new AdminAccount(
            UUID.randomUUID(),
            "browser-bound-admin",
            "브라우저 결합 관리자",
            AdminRole.ADMIN,
            AdminStatus.ACTIVE,
            "bootstrap"));
    when(oidcService.exchangeAndVerify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("browser-bound-admin"));
    Challenge challenge = issueChallenge();
    Cookie otherBrowser = new Cookie("cchaksa_admin_login", "different-browser-token");

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie(), otherBrowser)
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signInBody(challenge, challenge.state())))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("A10"));
    verifyNoInteractions(oidcService);
  }

  @Test
  void missingKakaoConfigurationFailsBeforeChallengeIsCreated() throws Exception {
    String javascriptAppKey = authProperties.getKakao().getJavascriptAppKey();
    authProperties.getKakao().setJavascriptAppKey(" ");

    try {
      mockMvc
          .perform(get("/api/admin/auth/challenge"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
          .andExpect(jsonPath("$.data").doesNotExist());
      assertThat(challengeRepository.count()).isZero();
    } finally {
      authProperties.getKakao().setJavascriptAppKey(javascriptAppKey);
    }
  }

  @Test
  void openApiPublishesAdminAuthenticationContract() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/admin/auth/challenge'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/signin'].post").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/me'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/signout'].post").exists())
            .andReturn();
    JsonNode schemas =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .path("components")
            .path("schemas");
    JsonNode challenge = schemas.path("AdminAuthChallengeResponse").path("properties");
    assertThat(challenge.has("challengeId")).isTrue();
    assertThat(challenge.has("nonce")).isTrue();
    assertThat(challenge.has("state")).isTrue();
    assertThat(challenge.has("javascriptAppKey")).isTrue();
    assertThat(challenge.has("redirectUri")).isTrue();
    assertThat(challenge.has("restApiKey")).isFalse();
    assertThat(challenge.has("clientSecret")).isFalse();
    JsonNode signIn = schemas.path("AdminAuthSignInRequest").path("properties");
    assertThat(signIn.has("challengeId")).isTrue();
    assertThat(signIn.has("authorizationCode")).isTrue();
    assertThat(signIn.has("state")).isTrue();
    assertThat(signIn.has("idToken")).isFalse();
    assertThat(signIn.has("redirectUri")).isFalse();
  }

  private Challenge issueChallenge() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/admin/auth/challenge"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.data.nonce").isNotEmpty())
            .andExpect(jsonPath("$.data.state").isNotEmpty())
            .andExpect(jsonPath("$.data.javascriptAppKey").value("test-admin-javascript-app-key"))
            .andExpect(jsonPath("$.data.redirectUri").value("http://localhost/login/callback"))
            .andReturn();
    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    Cookie loginCookie = result.getResponse().getCookie("cchaksa_admin_login");
    assertThat(csrfCookie.getPath()).isEqualTo("/");
    assertThat(loginCookie.isHttpOnly()).isTrue();
    assertThat(loginCookie.getPath()).isEqualTo("/api/admin/auth");
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return new Challenge(
        UUID.fromString(body.at("/data/challengeId").asText()),
        body.at("/data/state").asText(),
        csrfCookie,
        loginCookie);
  }

  private String signInBody(Challenge challenge, String state) {
    final String body =
        """
        {
          "challengeId": "%s",
          "authorizationCode": "authorization-code",
          "state": "%s"
        }
        """;
    return body.formatted(challenge.challengeId(), state);
  }

  private record Challenge(UUID challengeId, String state, Cookie csrfCookie, Cookie loginCookie) {}
}
