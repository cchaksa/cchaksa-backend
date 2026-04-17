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
import org.springframework.core.env.Environment;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobOutboxDispatcher {

    private static final List<ScrapeJobOutboxStatus> PUBLISHABLE_STATUSES = List.of(
            ScrapeJobOutboxStatus.PENDING,
            ScrapeJobOutboxStatus.RETRYABLE_FAILED
    );

    private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapeJobPublisher scrapeJobPublisher;
    private final ScrapingProperties scrapingProperties;
    private final MeterRegistry meterRegistry;
    private final Environment environment;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchEligibleOutboxes() {
        dispatchBatch("scheduled");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            Optional<ScrapeJobOutbox> preferred = scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(
                    preferredOutboxId,
                    PUBLISHABLE_STATUSES,
                    now
            );
            if (preferred.isEmpty()) {
                log.info("[BIZ] scrape.outbox.dispatch.preferred_missing trigger={} preferredOutboxId={} foundCount=0",
                        trigger, preferredOutboxId);
                log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId={} dispatchedCount=0",
                        trigger, preferredOutboxId);
                return 0;
            }

            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.success trigger={} preferredOutboxId={} foundCount=1",
                    trigger, preferredOutboxId);
            dispatchSingle(preferred.get(), now, trigger);
            log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId={} dispatchedCount={}",
                    trigger, preferredOutboxId, 1);
            return 1;
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
            List<ScrapeJobOutbox> outboxes = scrapeJobOutboxRepository.findPublishTargetsForUpdate(
                    PUBLISHABLE_STATUSES,
                    now,
                    PageRequest.of(0, batchSize)
            );
            log.info("[BIZ] scrape.outbox.dispatch.db_lookup.success trigger={} preferredOutboxId=null foundCount={}",
                    trigger, outboxes.size());

            int dispatched = 0;
            for (ScrapeJobOutbox outbox : outboxes) {
                dispatchSingle(outbox, now, trigger);
                dispatched++;
            }

            log.info("[BIZ] scrape.outbox.dispatch.end trigger={} preferredOutboxId=null dispatchedCount={}",
                    trigger, dispatched);
            return dispatched;
        } catch (RuntimeException exception) {
            logDispatchFailure(trigger, null, batchSize, now, exception);
            throw exception;
        }
    }

    private void dispatchSingle(ScrapeJobOutbox outbox, Instant attemptedAt, String trigger) {
        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
        if (job == null) {
            outbox.markDead("missing scrape job", attemptedAt);
            meterRegistry.counter("scrape.outbox.publish.fail").increment();
            meterRegistry.counter("scrape.outbox.dead").increment();
            log.error("[BIZ] scrape.outbox.dead trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} reason=missing_job",
                    trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
            return;
        }

        try {
            log.info("[BIZ] scrape.outbox.publish.start trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={}",
                    trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
            String queueMessageId = scrapeJobPublisher.publish(outbox.getPayloadJson());
            outbox.markSent(queueMessageId, attemptedAt);
            job.markRunning();
            meterRegistry.counter("scrape.outbox.publish.success").increment();
            log.info("[BIZ] scrape.outbox.sent trigger={} outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={}",
                    trigger, outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), queueMessageId);
        } catch (RuntimeException e) {
            handleFailure(outbox, job, attemptedAt, trigger, e);
        }
    }

    private void handleFailure(ScrapeJobOutbox outbox, ScrapeJob job, Instant attemptedAt, String trigger, RuntimeException exception) {
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

    private long calculateBackoffSeconds(int attemptCount) {
        long initial = scrapingProperties.getPublisher().getInitialBackoffSeconds();
        long max = scrapingProperties.getPublisher().getMaxBackoffSeconds();
        long calculated = initial * (1L << Math.max(0, attemptCount - 1));
        return Math.min(calculated, max);
    }

    private boolean isPermanentFailure(RuntimeException exception) {
        if (exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
            return true;
        }
        if (exception instanceof CannotCreateTransactionException || exception instanceof CannotAcquireLockException) {
            return false;
        }
        if (exception instanceof SqsException sqsException) {
            int statusCode = sqsException.statusCode();
            return statusCode >= 400 && statusCode < 500 && statusCode != 429;
        }
        return !(exception instanceof SdkClientException);
    }

    private String summarizeException(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
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
