// 관리자 Kakao token 교환 요청과 오류 매핑을 검증한다.

package com.chukchuk.haksa.domain.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import com.chukchuk.haksa.domain.admin.auth.service.AdminKakaoTokenClient;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class AdminKakaoTokenClientTests {
  private AdminAuthProperties properties;
  private MockRestServiceServer server;
  private AdminKakaoTokenClient client;

  @BeforeEach
  void setUp() {
    properties = new AdminAuthProperties();
    properties.getKakao().setJavascriptAppKey("javascript-app-key");
    properties.getKakao().setRestApiKey("rest-api-key");
    properties.getKakao().setClientSecret("client-secret");
    properties.getKakao().setRedirectUri("https://admin.cchaksa.com/login/callback");
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    client = new AdminKakaoTokenClient(restTemplate, properties);
  }

  @Test
  void exchangesCodeWithFixedServerConfiguration() {
    server
        .expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Content-Type", containsString("application/x-www-form-urlencoded")))
        .andExpect(
            content()
                .string(
                    allOf(
                        containsString("grant_type=authorization_code"),
                        containsString("client_id=rest-api-key"),
                        containsString("code=authorization-code"),
                        containsString("client_secret=client-secret"),
                        containsString(
                            "redirect_uri=https%3A%2F%2Fadmin.cchaksa.com%2Flogin%2Fcallback"))))
        .andRespond(withSuccess("{\"id_token\":\"signed-id-token\"}", MediaType.APPLICATION_JSON));

    assertThat(client.exchange("authorization-code")).isEqualTo("signed-id-token");
    server.verify();
  }

  @Test
  void mapsKakaoExchangeFailureToStableAuthenticationError() {
    server
        .expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> client.exchange("invalid-code"))
        .isInstanceOfSatisfying(
            CommonException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_KAKAO_AUTH_FAILED));
    server.verify();
  }

  @Test
  void mapsKakaoTimeoutToStableAuthenticationError() {
    server
        .expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
        .andRespond(
            request -> {
              throw new ResourceAccessException("timeout");
            });

    assertThatThrownBy(() -> client.exchange("authorization-code"))
        .isInstanceOfSatisfying(
            CommonException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_KAKAO_AUTH_FAILED));
    server.verify();
  }

  @Test
  void rejectsSuccessfulResponseWithoutIdToken() {
    server
        .expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
        .andRespond(withSuccess("{\"access_token\":\"unused\"}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.exchange("authorization-code"))
        .isInstanceOfSatisfying(
            CommonException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADMIN_KAKAO_AUTH_FAILED));
    server.verify();
  }
}
