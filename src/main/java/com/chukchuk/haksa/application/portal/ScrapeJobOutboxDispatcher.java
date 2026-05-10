package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobOutboxDispatcher {

    private static final int MAX_INLINE_PUBLISH_ATTEMPTS = 3;
    private static final long[] INLINE_PUBLISH_RETRY_DELAYS_MS = {200L, 500L};

    private static final List<ScrapeJobOutboxStatus> PUBLISHABLE_STATUSES = List.of(
            ScrapeJobOutboxStatus.PENDING,
            ScrapeJobOutboxStatus.RETRYABLE_FAILED
    );

    private final ScrapeJobOutboxDispatchTxService dispatchTxService;
    private final ScrapeJobPublisher scrapeJobPublisher;
    private final ScrapingProperties scrapingProperties;
    private final Environment environment;

    public void dispatchEligibleOutboxes() {
        dispatchBatch("scheduled");
    }

    public int dispatchOnce(String preferredOutboxId) {
        return dispatchPreferredOutbox("sync_request", preferredOutboxId);
    }

    private int dispatchPreferredOutbox(String trigger, String preferredOutboxId) {
        if (!scrapingProperties.getPublisher().isEnabled()) {
            log.info("[BIZ] scrape.outbox.dispatch.skip trigger={} reason=publisher_disabled preferredOutboxId={}",
                    trigger, preferredOutboxId);
            return 0;
        }

        Instant now = Instant.now();
        int batchSize = scrapingProperties.getPublisher().getBatchSize();
        log.info("[BIZ] scrape.outbox.dispatch.start trigger={} preferredOutboxId={} batchSize={} now={} activeProfiles={}",
                trigger, preferredOutboxId, batchSize, now, String.join(",", environment.getActiveProfiles()));

        try {
            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.start trigger={} preferredOutboxId={} batchSize={}",
                    trigger, preferredOutboxId, batchSize);
            ScrapeJobOutboxDispatchPlan plan = dispatchTxService.reservePreferred(
                    preferredOutboxId,
                    PUBLISHABLE_STATUSES,
                    now
            );
            if (plan.dispatchedCount() == 0) {
                log.info("[BIZ] scrape.outbox.dispatch.preferred_missing trigger={} preferredOutboxId={} foundCount=0",
                        trigger, preferredOutboxId);
                log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId={} dispatchedCount=0",
                        trigger, preferredOutboxId);
                return 0;
            }

            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.success trigger={} preferredOutboxId={} foundCount=1",
                    trigger, preferredOutboxId);
            publishCandidates(plan.candidates(), now, trigger);
            log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId={} dispatchedCount={}",
                    trigger, preferredOutboxId, plan.dispatchedCount());
            return plan.dispatchedCount();
        } catch (RuntimeException exception) {
            logDispatchFailure(trigger, preferredOutboxId, batchSize, now, exception);
            throw exception;
        }
    }

    private int dispatchBatch(String trigger) {
        if (!scrapingProperties.getPublisher().isEnabled()) {
            log.info("[BIZ] scrape.outbox.dispatch.skip trigger={} reason=publisher_disabled preferredOutboxId=null", trigger);
            return 0;
        }

        Instant now = Instant.now();
        int batchSize = scrapingProperties.getPublisher().getBatchSize();
        log.info("[BIZ] scrape.outbox.dispatch.start trigger={} preferredOutboxId=null batchSize={} now={} activeProfiles={}",
                trigger, batchSize, now, String.join(",", environment.getActiveProfiles()));

        try {
            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.start trigger={} preferredOutboxId=null batchSize={}",
                    trigger, batchSize);
            ScrapeJobOutboxDispatchPlan plan = dispatchTxService.reserveBatch(
                    PUBLISHABLE_STATUSES,
                    now,
                    batchSize
            );
            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.success trigger={} preferredOutboxId=null foundCount={}",
                    trigger, plan.dispatchedCount());

            publishCandidates(plan.candidates(), now, trigger);
            log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId=null dispatchedCount={}",
                    trigger, plan.dispatchedCount());
            return plan.dispatchedCount();
        } catch (RuntimeException exception) {
            logDispatchFailure(trigger, null, batchSize, now, exception);
            throw exception;
        }
    }

    private void publishCandidates(List<ScrapeJobOutboxPublishCandidate> candidates, Instant attemptedAt, String trigger) {
        for (ScrapeJobOutboxPublishCandidate candidate : candidates) {
            publishSingle(candidate, attemptedAt, trigger);
        }
    }

    private void publishSingle(ScrapeJobOutboxPublishCandidate candidate, Instant attemptedAt, String trigger) {
        try {
            log.info("[BIZ] scrape.outbox.publish.start trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={}",
                    trigger, candidate.outboxId(), candidate.jobId(), candidate.attemptCount(), candidate.status(), candidate.queueMessageId());
            String queueMessageId = publishWithBoundedRetry(candidate, trigger);
            dispatchTxService.markSent(candidate.outboxId(), queueMessageId, attemptedAt, trigger);
        } catch (RuntimeException e) {
            dispatchTxService.markFailed(candidate.outboxId(), attemptedAt, trigger, e);
        }
    }

    private String publishWithBoundedRetry(ScrapeJobOutboxPublishCandidate candidate, String trigger) {
        int publishAttempt = 1;
        while (true) {
            try {
                return scrapeJobPublisher.publish(candidate.payloadJson());
            } catch (RuntimeException exception) {
                if (isPermanentFailure(exception) || publishAttempt == MAX_INLINE_PUBLISH_ATTEMPTS) {
                    throw exception;
                }

                long delayMs = INLINE_PUBLISH_RETRY_DELAYS_MS[publishAttempt - 1];
                log.warn("[BIZ] scrape.outbox.publish.retry trigger={} outboxId={} jobId={} publishAttempt={} maxPublishAttempts={} delayMs={} exceptionClass={} message={}",
                        trigger,
                        candidate.outboxId(),
                        candidate.jobId(),
                        publishAttempt,
                        MAX_INLINE_PUBLISH_ATTEMPTS,
                        delayMs,
                        exception.getClass().getSimpleName(),
                        exception.getMessage());
                sleepBeforeRetry(delayMs, exception);
                publishAttempt++;
            }
        }
    }

    private void sleepBeforeRetry(long delayMs, RuntimeException publishException) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw publishException;
        }
    }

    private boolean isPermanentFailure(RuntimeException exception) {
        return ScrapeJobOutboxPublisherFailures.isPermanentFailure(exception);
    }

    private void logDispatchFailure(String trigger, String preferredOutboxId, int batchSize, Instant now, RuntimeException exception) {
        Throwable rootCause = rootCauseOf(exception);
        log.error("[BIZ] scrape.outbox.dispatch.fail trigger={} preferredOutboxId={} batchSize={} now={} activeProfiles={} exceptionClass={} rootCauseClass={} rootCauseMessage={}",
                trigger,
                preferredOutboxId,
                batchSize,
                now,
                Arrays.toString(environment.getActiveProfiles()),
                exception.getClass().getSimpleName(),
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage(),
                exception);
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor;
    }
}
