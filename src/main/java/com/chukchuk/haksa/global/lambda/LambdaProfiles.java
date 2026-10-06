package com.chukchuk.haksa.global.lambda;

import java.util.Arrays;

/** Lambda 실행에 사용하는 Spring profile 이름을 제공한다. */
public final class LambdaProfiles {

  static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
  static final String SPRING_PROFILES_ACTIVE_ENV = "SPRING_PROFILES_ACTIVE";
  static final String DEFAULT_ACTIVE_PROFILE = "develop-shadow";

  private LambdaProfiles() {}

  /**
   * 시스템 속성, 환경 변수, 기본값 순으로 Lambda의 활성 Spring profile을 결정한다.
   *
   * @return 쉼표로 구분된 설정을 공백 제거 후 분리한 profile 이름 배열
   */
  public static String[] resolveActiveProfiles() {
    String configuredProfiles = System.getProperty(SPRING_PROFILES_ACTIVE);
    if (configuredProfiles == null || configuredProfiles.isBlank()) {
      configuredProfiles = System.getenv(SPRING_PROFILES_ACTIVE_ENV);
    }
    if (configuredProfiles == null || configuredProfiles.isBlank()) {
      configuredProfiles = DEFAULT_ACTIVE_PROFILE;
    }

    return Arrays.stream(configuredProfiles.split(","))
        .map(String::trim)
        .filter(profile -> !profile.isEmpty())
        .toArray(String[]::new);
  }
}
