package com.chukchuk.haksa.global.config;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import com.chukchuk.haksa.infrastructure.oidc.KakaoOidcService;
import com.chukchuk.haksa.domain.user.service.OidcService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OIDCConfig {

    @Bean
    public Map<OidcProvider, OidcService> oidcServices(
            KakaoOidcService kakaoOidcService
    ) {
        return Map.of(
                OidcProvider.KAKAO, kakaoOidcService
        );
    }
}
