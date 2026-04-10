package com.chukchuk.haksa.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@RequiredArgsConstructor
public class ScrapeJobDispatchConfig {

    private final ScrapingProperties scrapingProperties;

    @Bean(name = "scrapeOutboxDispatchExecutor")
    public TaskExecutor scrapeOutboxDispatchExecutor() {
        ScrapingProperties.Publisher.AfterCommit afterCommit = scrapingProperties.getPublisher().getAfterCommit();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("scrape-outbox-dispatch-");
        executor.setCorePoolSize(afterCommit.getExecutorCorePoolSize());
        executor.setMaxPoolSize(afterCommit.getExecutorMaxPoolSize());
        executor.setQueueCapacity(afterCommit.getQueueCapacity());
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "scrapeOutboxRetryScheduler")
    public TaskScheduler scrapeOutboxRetryScheduler() {
        ScrapingProperties.Publisher.AfterCommit afterCommit = scrapingProperties.getPublisher().getAfterCommit();
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadNamePrefix("scrape-outbox-retry-");
        scheduler.setPoolSize(Math.max(afterCommit.getExecutorCorePoolSize(), 2));
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public S3Client scrapeResultS3Client() {
        ScrapingProperties.Callback.ResultStore store = scrapingProperties.getCallback().getResultStore();
        return S3Client.builder()
                .region(Region.of(store.getRegion()))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(java.time.Duration.ofSeconds(store.getApiCallTimeoutSeconds()))
                        .apiCallAttemptTimeout(java.time.Duration.ofSeconds(store.getApiCallAttemptTimeoutSeconds()))
                        .build())
                .build();
    }
}
