package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobOutboxDispatchTxService {

    private static final long PUBLISH_LEASE_SECONDS = 30;

    private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapingProperties scrapingProperties;
    private final MeterRegistry meterRegistry;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScrapeJobOutboxDispatchPlan reservePreferred(
            String preferredOutboxId,
            Collection<ScrapeJobOutboxStatus> publishableStatuses,
            Instant now
    ) {
        Optional<ScrapeJobOutbox> preferred = scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(
                preferredOutboxId,
                publishableStatuses,
                now
        );
        if (preferred.isEmpty()) {
            return ScrapeJobOutboxDispatchPlan.empty();
        }
        return buildPlan(List.of(preferred.get()), now, "sync_request");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScrapeJobOutboxDispatchPlan reserveBatch(
            Collection<ScrapeJobOutboxStatus> publishableStatuses,
            Instant now,
            int batchSize
    ) {
        List<ScrapeJobOutbox> outboxes = scrapeJobOutboxRepository.findPublishTargetsForUpdate(
                publishableStatuses,
                now,
                PageRequest.of(0, batchSize)
        );
        return buildPlan(outboxes, now, "scheduled");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(String outboxId, String queueMessageId, Instant attemptedAt, String trigger) {
        ScrapeJobOutbox outbox = scrapeJobOutboxRepository.findForUpdateByOutboxId(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() == ScrapeJobOutboxStatus.SENT) {
            return;
        }

        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
        if (job == null) {
            markMissingJob(outbox, attemptedAt, trigger);
            return;
        }

        outbox.markSent(queueMessageId, attemptedAt);
        job.markRunning();
        meterRegistry.counter("scrape.outbox.publish.success").increment();
        log.info("[BIZ] scrape.outbox.sent trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={}",
                trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), queueMessageId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String outboxId, Instant attemptedAt, String trigger, RuntimeException exception) {
        ScrapeJobOutbox outbox = scrapeJobOutboxRepository.findForUpdateByOutboxId(outboxId).orElse(null);
        if (outbox == null || outbox.getStatus() == ScrapeJobOutboxStatus.SENT) {
            return;
        }

        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
        if (job == null) {
            markMissingJob(outbox, attemptedAt, trigger);
            return;
        }

        meterRegistry.counter("scrape.outbox.publish.fail").increment();
        boolean permanentFailure = isPermanentFailure(exception);
        boolean maxAttemptsReached = outbox.getAttemptCount() + 1 >= scrapingProperties.getPublisher().getMaxAttempts();
        String summary = summarizeException(exception);

        if (permanentFailure || maxAttemptsReached) {
            outbox.markDead(summary, attemptedAt);
            if (!job.isCompleted()) {
                job.markFailed(
                        ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED.name(),
                        ErrorCode.SCRAPE_JOB_OUTBOX_DEAD.message(),
                        true,
                        attemptedAt
                );
            }
            meterRegistry.counter("scrape.outbox.dead").increment();
            log.error("[BIZ] scrape.outbox.dead trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} reason={}",
                    trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId(), summary);
            return;
        }

        Instant nextAttemptAt = attemptedAt.plusSeconds(calculateBackoffSeconds(outbox.getAttemptCount() + 1));
        outbox.markRetryableFailure(summary, attemptedAt, nextAttemptAt);
        meterRegistry.counter("scrape.outbox.retry").increment();
        log.warn("[BIZ] scrape.outbox.retry trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} nextAttemptAt={} reason={}",
                trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId(), nextAttemptAt, summary);
    }

    private ScrapeJobOutboxDispatchPlan buildPlan(List<ScrapeJobOutbox> outboxes, Instant now, String trigger) {
        List<ScrapeJobOutboxPublishCandidate> candidates = new ArrayList<>();
        for (ScrapeJobOutbox outbox : outboxes) {
            ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
            if (job == null) {
                markMissingJob(outbox, now, trigger);
                continue;
            }

            outbox.reserveForPublish(now.plusSeconds(PUBLISH_LEASE_SECONDS), now);
            candidates.add(new ScrapeJobOutboxPublishCandidate(
                    outbox.getOutboxId(),
                    outbox.getJobId(),
                    outbox.getPayloadJson(),
                    outbox.getAttemptCount(),
                    outbox.getStatus(),
                    outbox.getQueueMessageId()
            ));
        }
        return new ScrapeJobOutboxDispatchPlan(candidates, outboxes.size());
    }

    private void markMissingJob(ScrapeJobOutbox outbox, Instant attemptedAt, String trigger) {
        outbox.markDead("missing scrape job", attemptedAt);
        meterRegistry.counter("scrape.outbox.publish.fail").increment();
        meterRegistry.counter("scrape.outbox.dead").increment();
        log.error("[BIZ] scrape.outbox.dead trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} reason=missing_job",
                trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
    }

    private long calculateBackoffSeconds(int attemptCount) {
        long initial = scrapingProperties.getPublisher().getInitialBackoffSeconds();
        long max = scrapingProperties.getPublisher().getMaxBackoffSeconds();
        long calculated = initial * (1L << Math.max(0, attemptCount - 1));
        return Math.min(calculated, max);
    }

    private boolean isPermanentFailure(RuntimeException exception) {
        return ScrapeJobOutboxPublisherFailures.isPermanentFailure(exception);
    }

    private String summarizeException(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }
}
