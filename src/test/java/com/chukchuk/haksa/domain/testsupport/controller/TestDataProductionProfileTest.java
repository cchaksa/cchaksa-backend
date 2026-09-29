// 운영 프로필에서 테스트 데이터 API 빈이 등록되지 않는지 검증한다.

package com.chukchuk.haksa.domain.testsupport.controller;

import com.chukchuk.haksa.domain.testsupport.service.TestAccountService;
import com.chukchuk.haksa.domain.testsupport.service.TestLectureEvaluationService;
import com.chukchuk.haksa.domain.testsupport.service.TestMutationService;
import com.chukchuk.haksa.domain.testsupport.service.TestOptionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class TestDataProductionProfileTest {
  @Test
  void productionDoesNotRegisterTestDataApiBeans() {
    new WebApplicationContextRunner()
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
        .withUserConfiguration(
            TestDataController.class,
            TestAccountService.class,
            TestLectureEvaluationService.class,
            TestMutationService.class,
            TestOptionService.class)
        .run(
            context ->
                org.assertj.core.api.Assertions.assertThat(context)
                    .doesNotHaveBean(TestDataController.class)
                    .doesNotHaveBean(TestAccountService.class)
                    .doesNotHaveBean(TestLectureEvaluationService.class)
                    .doesNotHaveBean(TestMutationService.class)
                    .doesNotHaveBean(TestOptionService.class));
  }
}
