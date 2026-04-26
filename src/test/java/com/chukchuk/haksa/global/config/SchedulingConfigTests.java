package com.chukchuk.haksa.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class);

    @Test
    void taskSchedulerIsDisabledByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(ThreadPoolTaskScheduler.class)
        );
    }

    @Test
    void taskSchedulerIsCreatedWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("scraping.scheduler.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class));
    }
}
