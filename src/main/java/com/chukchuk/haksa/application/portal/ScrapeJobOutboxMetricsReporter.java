package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobOutboxMetricsReporter {

    private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
    private final MeterRegistry meterRegistry;
    private final ScrapingProperties scrapingProperties;
    private final TaskScheduler taskScheduler;

    private final AtomicLong deadCount = new AtomicLong();
    private final AtomicLong retryableCount = new AtomicLong();
    private ScheduledFuture<?> refreshFuture;

    @PostConstruct
    void init() {
        meterRegistry.gauge("scrape.outbox.dead.count", deadCount);
        meterRegistry.gauge("scrape.outbox.retryable_failed.count", retryableCount);
        long refreshMs = scrapingProperties.getPublisher().getMetricsRefreshMs();
        refreshFuture = taskScheduler.scheduleAtFixedRate(this::refreshSafely, refreshMs);
    }

    @PreDestroy
    void shutdown() {
        if (refreshFuture != null) {
            refreshFuture.cancel(true);
        }
    }

    private void refreshSafely() {
        try {
            deadCount.set(scrapeJobOutboxRepository.countByStatus(ScrapeJobOutboxStatus.DEAD));
            retryableCount.set(scrapeJobOutboxRepository.countByStatus(ScrapeJobOutboxStatus.RETRYABLE_FAILED));
        } catch (Exception exception) {
            log.warn("[OBS] scrape.outbox.metrics.refresh.fail", exception);
        }
    }
}
