package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;

import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

class ScrapeJobOutboxMetricsReporterTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BaseTestConfig.class);

    @Test
    void taskSchedulerBean이_없으면_reporter를_생성하지_않는다() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(ScrapeJobOutboxMetricsReporter.class);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void taskScheduler가_있으면_reporter가_주기_갱신을_등록한다() {
        ScrapeJobOutboxRepository repository = mock(ScrapeJobOutboxRepository.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ScheduledFuture<Object> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).scheduleAtFixedRate(any(Runnable.class), anyLong());

        ScrapeJobOutboxMetricsReporter reporter = new ScrapeJobOutboxMetricsReporter(
                repository,
                new SimpleMeterRegistry(),
                new ScrapingProperties(),
                taskScheduler
        );

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
