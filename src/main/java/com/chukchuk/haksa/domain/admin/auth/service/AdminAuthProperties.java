package com.chukchuk.haksa.domain.admin.auth.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 일반 사용자 인증과 분리된 관리자 인증 설정이다. */
@Component
@ConfigurationProperties(prefix = "security.admin")
@Getter
@Setter
public class AdminAuthProperties {
  private final Kakao kakao = new Kakao();
  private Duration challengeTtl = Duration.ofMinutes(5);
  private Duration idleTimeout = Duration.ofMinutes(30);
  private Duration absoluteTimeout = Duration.ofHours(8);
  private boolean cookieSecure = true;

  /** 관리자 전용 Kakao 앱 설정이다. */
  @Getter
  @Setter
  public static class Kakao {
    private String javascriptAppKey;
    private String restApiKey;
    private String clientSecret;
    private String redirectUri;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
  }
}
