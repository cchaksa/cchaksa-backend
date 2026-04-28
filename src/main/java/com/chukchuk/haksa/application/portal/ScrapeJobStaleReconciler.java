package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobStaleReconciler {

    private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapingProperties scrapingProperties;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${scraping.stale.fixed-delay-ms:60000}")
    @Transactional
    public void reconcileStaleQueuedJobs() {
        if (!scrapingProperties.getStale().isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(scrapingProperties.getStale().getTimeoutSeconds());
        List<ScrapeJobOutbox> staleOutboxes = scrapeJobOutboxRepository.findStaleSentTargetsForUpdate(
                ScrapeJobOutboxStatus.SENT,
                threshold,
                ScrapeJobStatus.RUNNING,
                PageRequest.of(0, scrapingProperties.getStale().getBatchSize())
        );

        for (ScrapeJobOutbox outbox : staleOutboxes) {
            ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
            if (job == null || job.isCompleted()) {
                continue;
            }

            job.markFailed(
                    ErrorCode.CALLBACK_TIMEOUT.name(),
                    ErrorCode.CALLBACK_TIMEOUT.message(),
                    true,
                    now
            );
            meterRegistry.counter("scrape.job.callback.timeout").increment();
            recordQueuedAge(job, now);
            log.warn("[BIZ] scrape.job.callback.timeout jobId={} outboxId={} attempt={} outboxStatus={} queueMessageId={}",
                    job.getJobId(), outbox.getOutboxId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
        }
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null) {
            return;
        }
        double queuedAgeSeconds = Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
        meterRegistry.summary("scrape.job.queued.age.seconds").record(queuedAgeSeconds);
    }
}
