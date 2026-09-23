// 운영 프로필에서 테스트 어드민 컨트롤러가 등록되지 않는지 검증한다.

package com.chukchuk.haksa.domain.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class AdminTestProductionProfileTest {
  @Test
  void productionDoesNotRegisterTestAdminEndpoints() {
    new WebApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
        .withUserConfiguration(AdminTestController.class)
        .run(
            context ->
                org.assertj.core.api.Assertions.assertThat(context)
                    .doesNotHaveBean(AdminTestController.class));
  }
}
