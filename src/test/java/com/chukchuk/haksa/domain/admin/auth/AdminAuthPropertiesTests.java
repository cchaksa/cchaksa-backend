package com.chukchuk.haksa.domain.admin.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chukchuk.haksa.domain.admin.auth.service.AdminAuthProperties;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AdminAuthPropertiesTests {
  private AdminAuthProperties properties;

  @BeforeEach
  void setUp() {
    properties = new AdminAuthProperties();
    properties.getKakao().setJavascriptAppKey("javascript-app-key");
    properties.getKakao().setRestApiKey("rest-api-key");
    properties.getKakao().setRedirectUri("https://admin.example.com/login/callback");
  }

  @ParameterizedTest
  @MethodSource("missingRequiredSettings")
  void rejectsMissingRequiredKakaoSetting(Consumer<AdminAuthProperties.Kakao> removeSetting) {
    removeSetting.accept(properties.getKakao());

    assertThatThrownBy(properties::requireKakaoLoginConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Admin Kakao login configuration is incomplete");
  }

  @Test
  void allowsClientSecretToBeOmitted() {
    properties.getKakao().setClientSecret(null);

    assertThat(properties.requireKakaoLoginConfiguration()).isSameAs(properties.getKakao());
  }

  private static Stream<Consumer<AdminAuthProperties.Kakao>> missingRequiredSettings() {
    return Stream.of(
        kakao -> kakao.setJavascriptAppKey(" "),
        kakao -> kakao.setRestApiKey(null),
        kakao -> kakao.setRedirectUri(""));
  }
}
