// 관리자 Kakao authorization code의 서버 측 token 교환을 담당한다.

package com.chukchuk.haksa.domain.admin.auth.service;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** 관리자 Kakao authorization code를 서버에서 ID token으로 교환한다. */
@Component
public class AdminKakaoTokenClient {
  private static final URI TOKEN_ENDPOINT = URI.create("https://kauth.kakao.com/oauth/token");

  private final RestTemplate restTemplate;
  private final AdminAuthProperties properties;

  /**
   * 관리자 Kakao token 교환 클라이언트를 생성한다.
   *
   * @param restTemplate 외부 HTTP 요청 클라이언트
   * @param properties 관리자 인증 설정
   */
  public AdminKakaoTokenClient(
      @Qualifier("adminKakaoRestTemplate") RestTemplate restTemplate,
      AdminAuthProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  /**
   * 고정된 관리자 앱 설정으로 authorization code를 교환한다.
   *
   * @param authorizationCode Kakao가 redirect URI에 전달한 일회성 code
   * @return Kakao OIDC ID token
   * @throws IllegalStateException 필수 설정이 없는 경우
   * @throws CommonException Kakao token 교환에 실패한 경우
   */
  public String exchange(String authorizationCode) {
    AdminAuthProperties.Kakao kakao = properties.requireKakaoLoginConfiguration();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("client_id", kakao.getRestApiKey());
    form.add("redirect_uri", kakao.getRedirectUri());
    form.add("code", authorizationCode);
    if (StringUtils.hasText(kakao.getClientSecret())) {
      form.add("client_secret", kakao.getClientSecret());
    }

    try {
      TokenResponse response =
          restTemplate.postForObject(
              TOKEN_ENDPOINT, new HttpEntity<>(form, headers), TokenResponse.class);
      if (response == null || !StringUtils.hasText(response.idToken())) {
        throw new CommonException(ErrorCode.ADMIN_KAKAO_AUTH_FAILED);
      }
      return response.idToken();
    } catch (CommonException exception) {
      throw exception;
    } catch (RestClientException exception) {
      throw new CommonException(ErrorCode.ADMIN_KAKAO_AUTH_FAILED, exception);
    }
  }

  private record TokenResponse(@JsonProperty("id_token") String idToken) {}
}
