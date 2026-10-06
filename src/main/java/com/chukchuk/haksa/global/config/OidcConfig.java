// OIDC 공급자별 인증 서비스와 HTTP 클라이언트를 구성한다.

package com.chukchuk.haksa.global.config;

import com.chukchuk.haksa.domain.user.service.OidcService;
import com.chukchuk.haksa.global.security.service.OidcProvider;
import com.chukchuk.haksa.infrastructure.oidc.AppleOidcService;
import com.chukchuk.haksa.infrastructure.oidc.KakaoOidcService;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** OIDC 인증에 필요한 애플리케이션 구성을 제공한다. */
@Configuration
public class OidcConfig {

  /**
   * 공급자별 OIDC 인증 서비스를 구성한다.
   *
   * @param kakaoOidcService 카카오 OIDC 인증 서비스
   * @param appleOidcService 애플 OIDC 인증 서비스
   * @return 공급자별 OIDC 인증 서비스
   */
  @Bean
  public Map<OidcProvider, OidcService> oidcServices(
      KakaoOidcService kakaoOidcService, AppleOidcService appleOidcService) {
    return Map.of(
        OidcProvider.KAKAO, kakaoOidcService,
        OidcProvider.APPLE, appleOidcService);
  }

  /**
   * OIDC 공급자 API 호출에 사용할 HTTP 클라이언트를 구성한다.
   *
   * @param builder Spring HTTP 클라이언트 빌더
   * @return OIDC 공급자 요청에 사용할 HTTP 클라이언트
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }
}
