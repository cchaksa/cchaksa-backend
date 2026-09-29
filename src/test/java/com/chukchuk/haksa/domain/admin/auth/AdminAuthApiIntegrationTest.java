package com.chukchuk.haksa.domain.admin.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chukchuk.haksa.domain.admin.auth.model.AdminAccount;
import com.chukchuk.haksa.domain.admin.auth.model.AdminRole;
import com.chukchuk.haksa.domain.admin.auth.model.AdminStatus;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminAccountRepository;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminLoginChallengeRepository;
import com.chukchuk.haksa.domain.admin.auth.repository.AdminSessionRepository;
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
    when(oidcService.verify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("kakao-cs-1"));

    Challenge challenge = issueChallenge();
    MvcResult signIn =
        mockMvc
            .perform(
                post("/api/admin/auth/signin")
                    .cookie(challenge.csrfCookie())
                    .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"challengeId\":\"%s\",\"idToken\":\"id-token\"}"
                            .formatted(challenge.challengeId())))
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
    when(oidcService.verify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("not-registered"));
    Challenge challenge = issueChallenge();

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"challengeId\":\"%s\",\"idToken\":\"id-token\"}"
                        .formatted(challenge.challengeId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("A09"));

    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(challenge.csrfCookie())
                .header("X-XSRF-TOKEN", challenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"challengeId\":\"%s\",\"idToken\":\"id-token\"}"
                        .formatted(challenge.challengeId())))
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
    when(oidcService.verify(anyString(), anyString()))
        .thenReturn(Jwts.claims().setSubject("inactive-admin"));
    Challenge inactiveChallenge = issueChallenge();
    mockMvc
        .perform(
            post("/api/admin/auth/signin")
                .cookie(inactiveChallenge.csrfCookie())
                .header("X-XSRF-TOKEN", inactiveChallenge.csrfCookie().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"challengeId\":\"%s\",\"idToken\":\"id-token\"}"
                        .formatted(inactiveChallenge.challengeId())))
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
  void openApiPublishesAdminAuthenticationContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/admin/auth/challenge'].get").exists())
        .andExpect(jsonPath("$.paths['/api/admin/auth/signin'].post").exists())
        .andExpect(jsonPath("$.paths['/api/admin/auth/me'].get").exists())
        .andExpect(jsonPath("$.paths['/api/admin/auth/signout'].post").exists());
  }

  private Challenge issueChallenge() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/admin/auth/challenge"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nonce").isNotEmpty())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return new Challenge(
        UUID.fromString(body.at("/data/challengeId").asText()),
        result.getResponse().getCookie("XSRF-TOKEN"));
  }

  private record Challenge(UUID challengeId, Cookie csrfCookie) {}
}
