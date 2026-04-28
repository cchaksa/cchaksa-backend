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

    @Transactional
    public int reconcileStaleQueuedJobs() {
        if (!scrapingProperties.getStale().isEnabled()) {
            return 0;
        }

        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(scrapingProperties.getStale().getTimeoutSeconds());
        List<ScrapeJobOutbox> staleOutboxes = scrapeJobOutboxRepository.findStaleSentTargetsForUpdate(
                ScrapeJobOutboxStatus.SENT,
                threshold,
                ScrapeJobStatus.RUNNING,
                PageRequest.of(0, scrapingProperties.getStale().getBatchSize())
        );

        int affectedCount = 0;
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
            affectedCount++;
            log.warn("[BIZ] scrape.job.callback.timeout jobId={} outboxId={} attempt={} outboxStatus={} queueMessageId={}",
                    job.getJobId(), outbox.getOutboxId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
        }
        return affectedCount;
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null) {
            return;
        }
        double queuedAgeSeconds = Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
        meterRegistry.summary("scrape.job.queued.age.seconds").record(queuedAgeSeconds);
    }
}
