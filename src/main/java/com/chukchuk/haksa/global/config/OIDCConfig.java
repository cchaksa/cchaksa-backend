package com.chukchuk.haksa.global.config;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import com.chukchuk.haksa.infrastructure.oidc.AppleOidcService;
import com.chukchuk.haksa.infrastructure.oidc.KakaoOidcService;
import com.chukchuk.haksa.domain.user.service.OidcService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Configuration
public class OIDCConfig {

    @Bean
    public Map<OidcProvider, OidcService> oidcServices(
            KakaoOidcService kakaoOidcService,
            AppleOidcService appleOidcService
    ) {
        return Map.of(
                OidcProvider.KAKAO, kakaoOidcService,
                OidcProvider.APPLE, appleOidcService
        );
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
