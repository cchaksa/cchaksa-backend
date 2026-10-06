package com.chukchuk.haksa.domain.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.chukchuk.haksa.domain.admin.auth.repository.AdminSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthApiIntegrationTest {
  private static final String PASSWORD = "initial password";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AdminAccountRepository accountRepository;
  @Autowired private AdminSessionRepository sessionRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void clean() {
    sessionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void csrfLoginMeAndSignoutUseProtectedAdminSession() throws Exception {
    createAccount("cs.agent", PASSWORD, AdminStatus.ACTIVE);
    Cookie csrf = issueCsrf();

    MvcResult signIn = signIn("cs.agent", PASSWORD, csrf, status().isOk());
    Cookie session = signIn.getResponse().getCookie("cchaksa_admin_session");
    assertThat(session).isNotNull();
    assertThat(session.isHttpOnly()).isTrue();
    assertThat(session.getPath()).isEqualTo("/api/admin");
    assertThat(signIn.getRequest().getSession(false)).isNull();

    mockMvc
        .perform(get("/api/admin/auth/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName").value("CS 담당자"))
        .andExpect(jsonPath("$.data.adminRole").value("CS_AGENT"));

    mockMvc
        .perform(post("/api/admin/auth/signout").cookie(session))
        .andExpect(status().isForbidden());
    MvcResult signOut =
        mockMvc
            .perform(
                post("/api/admin/auth/signout")
                    .cookie(session, csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue()))
            .andExpect(status().isNoContent())
            .andReturn();
    assertThat(signOut.getResponse().getCookie("XSRF-TOKEN").getMaxAge()).isZero();
    mockMvc.perform(get("/api/admin/auth/me").cookie(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownWrongPasswordAndInactiveAccountShareOneError() throws Exception {
    createAccount("inactive.admin", PASSWORD, AdminStatus.INACTIVE);
    createAccount("active.admin", PASSWORD, AdminStatus.ACTIVE);
    Cookie csrf = issueCsrf();

    assertInvalidCredentials(signIn("unknown.admin", PASSWORD, csrf, status().isUnauthorized()));
    assertInvalidCredentials(
        signIn("active.admin", "wrong-password", csrf, status().isUnauthorized()));
    assertInvalidCredentials(signIn("inactive.admin", PASSWORD, csrf, status().isUnauthorized()));
  }

  @Test
  void signinUsesCaseSensitiveExactLoginIdAndRejectsBlankValues() throws Exception {
    createAccount("Case.Sensitive", PASSWORD, AdminStatus.ACTIVE);
    Cookie csrf = issueCsrf();

    assertInvalidCredentials(signIn("case.sensitive", PASSWORD, csrf, status().isUnauthorized()));
    signIn("Case.Sensitive", PASSWORD, csrf, status().isOk());
    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"valid.id\",\"password\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
  }

  @Test
  void passwordChangeRotatesCurrentSessionAndRevokesOtherSessions() throws Exception {
    createAccount("admin.one", PASSWORD, AdminStatus.ACTIVE);
    Cookie csrf = issueCsrf();
    Cookie first =
        signIn("admin.one", PASSWORD, csrf, status().isOk())
            .getResponse()
            .getCookie("cchaksa_admin_session");
    Cookie second =
        signIn("admin.one", PASSWORD, csrf, status().isOk())
            .getResponse()
            .getCookie("cchaksa_admin_session");

    MvcResult changed =
        mockMvc
            .perform(
                post("/api/admin/auth/password")
                    .cookie(first, csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(passwordChangeBody(PASSWORD, "new password")))
            .andExpect(status().isNoContent())
            .andReturn();
    Cookie rotated = changed.getResponse().getCookie("cchaksa_admin_session");
    assertThat(rotated).isNotNull();
    assertThat(rotated.getValue()).isNotEqualTo(first.getValue());
    assertThat(changed.getResponse().getCookie("XSRF-TOKEN")).isNull();
    assertThat(changed.getRequest().getSession(false)).isNull();

    mockMvc.perform(get("/api/admin/auth/me").cookie(first)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/admin/auth/me").cookie(second)).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/admin/auth/me").cookie(rotated)).andExpect(status().isOk());
    assertInvalidCredentials(signIn("admin.one", PASSWORD, csrf, status().isUnauthorized()));
    signIn("admin.one", "new password", csrf, status().isOk());
  }

  @Test
  void passwordChangeRejectsMismatchAndUnchangedPassword() throws Exception {
    createAccount("admin.two", PASSWORD, AdminStatus.ACTIVE);
    Cookie csrf = issueCsrf();
    Cookie session =
        signIn("admin.two", PASSWORD, csrf, status().isOk())
            .getResponse()
            .getCookie("cchaksa_admin_session");

    assertPasswordError(session, csrf, "wrong", "new password", "A14");
    assertPasswordError(session, csrf, PASSWORD, PASSWORD, "A15");
    mockMvc
        .perform(
            post("/api/admin/auth/password")
                .cookie(session, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordChangeBody(PASSWORD, "   ")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("C01"));
  }

  @Test
  void unauthenticatedIsUnauthorizedAndRegularUserIsForbidden() throws Exception {
    mockMvc
        .perform(get("/api/admin/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("A05"));
    mockMvc
        .perform(get("/api/admin/auth/me").with(user("regular-user")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("C04"));
  }

  @Test
  void openApiPublishesLocalAdminAuthenticationContract() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/admin/auth/csrf'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/signin'].post").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/password'].post").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/me'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/signout'].post").exists())
            .andExpect(jsonPath("$.paths['/api/admin/auth/challenge']").doesNotExist())
            .andReturn();
    JsonNode schemas =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .path("components")
            .path("schemas");
    JsonNode signIn = schemas.path("AdminAuthSignInRequest").path("properties");
    assertThat(signIn.has("loginId")).isTrue();
    assertThat(signIn.has("password")).isTrue();
    assertThat(signIn.has("challengeId")).isFalse();
    assertThat(signIn.has("authorizationCode")).isFalse();
    JsonNode password = schemas.path("AdminPasswordChangeRequest").path("properties");
    assertThat(password.has("currentPassword")).isTrue();
    assertThat(password.has("newPassword")).isTrue();
  }

  private AdminAccount createAccount(String loginId, String password, AdminStatus status) {
    return accountRepository.save(
        new AdminAccount(
            UUID.randomUUID(),
            loginId,
            passwordEncoder.encode(password),
            "CS 담당자",
            AdminRole.CS_AGENT,
            status,
            "bootstrap"));
  }

  private Cookie issueCsrf() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/admin/auth/csrf"))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn();
    Cookie csrf = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrf).isNotNull();
    assertThat(csrf.getPath()).isEqualTo("/");
    return csrf;
  }

  private MvcResult signIn(
      String loginId,
      String password,
      Cookie csrf,
      org.springframework.test.web.servlet.ResultMatcher expectedStatus)
      throws Exception {
    return mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        java.util.Map.of("loginId", loginId, "password", password))))
        .andExpect(expectedStatus)
        .andReturn();
  }

  private void assertInvalidCredentials(MvcResult result) throws Exception {
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(body.at("/error/code").asText()).isEqualTo("A13");
  }

  private void assertPasswordError(
      Cookie session, Cookie csrf, String currentPassword, String newPassword, String code)
      throws Exception {
    mockMvc
        .perform(
            post("/api/admin/auth/password")
                .cookie(session, csrf)
                .header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(passwordChangeBody(currentPassword, newPassword)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value(code));
  }

  private String passwordChangeBody(String currentPassword, String newPassword) throws Exception {
    return objectMapper.writeValueAsString(
        java.util.Map.of("currentPassword", currentPassword, "newPassword", newPassword));
  }
}
