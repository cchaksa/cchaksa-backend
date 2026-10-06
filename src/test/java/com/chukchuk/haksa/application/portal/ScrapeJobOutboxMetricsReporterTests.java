package com.chukchuk.haksa.application.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;

class ScrapeJobOutboxMetricsReporterTests {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(BaseTestConfig.class);

  @Test
  @DisplayName("TaskScheduler 빈이 없으면 reporter를 생성하지 않는다")
  void doesNotCreateReporterWithoutTaskSchedulerBean() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(ScrapeJobOutboxMetricsReporter.class);
        });
  }

  @Test
  @DisplayName("TaskScheduler가 있으면 reporter가 주기 갱신을 등록한다")
  @SuppressWarnings("unchecked")
  void registersPeriodicRefreshWhenTaskSchedulerExists() {
    ScrapeJobOutboxRepository repository = mock(ScrapeJobOutboxRepository.class);
    TaskScheduler taskScheduler = mock(TaskScheduler.class);
    ScheduledFuture<Object> future = mock(ScheduledFuture.class);
    doReturn(future).when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), anyLong());

    ScrapeJobOutboxMetricsReporter reporter =
        new ScrapeJobOutboxMetricsReporter(
            repository, new SimpleMeterRegistry(), new ScrapingProperties(), taskScheduler);

    reporter.init();
    reporter.shutdown();

    verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), anyLong());
    verify(future).cancel(true);
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ScrapeJobOutboxMetricsReporter.class)
  static class BaseTestConfig {

    @Bean
    ScrapeJobOutboxRepository scrapeJobOutboxRepository() {
      return mock(ScrapeJobOutboxRepository.class);
    }

    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    ScrapingProperties scrapingProperties() {
      return new ScrapingProperties();
    }
  }
}
