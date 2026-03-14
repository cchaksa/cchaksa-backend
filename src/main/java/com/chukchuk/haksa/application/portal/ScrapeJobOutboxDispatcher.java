package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.List;

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

    @PostConstruct
    void registerGauges() {
        meterRegistry.gauge("scrape.outbox.dead.count", scrapeJobOutboxRepository,
                repository -> (double) repository.countByStatus(ScrapeJobOutboxStatus.DEAD));
        meterRegistry.gauge("scrape.outbox.retryable_failed.count", scrapeJobOutboxRepository,
                repository -> (double) repository.countByStatus(ScrapeJobOutboxStatus.RETRYABLE_FAILED));
    }

    @Scheduled(fixedDelayString = "${scraping.publisher.fixed-delay-ms:10000}")
    @Transactional
    public void dispatchEligibleOutboxes() {
        if (!scrapingProperties.getPublisher().isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        List<ScrapeJobOutbox> outboxes = scrapeJobOutboxRepository.findPublishTargetsForUpdate(
                PUBLISHABLE_STATUSES,
                now,
                PageRequest.of(0, scrapingProperties.getPublisher().getBatchSize())
        );

        for (ScrapeJobOutbox outbox : outboxes) {
            dispatchSingle(outbox, now);
        }
    }

    private void dispatchSingle(ScrapeJobOutbox outbox, Instant attemptedAt) {
        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(outbox.getJobId()).orElse(null);
        if (job == null) {
            outbox.markDead("missing scrape job", attemptedAt);
            meterRegistry.counter("scrape.outbox.publish.fail").increment();
            meterRegistry.counter("scrape.outbox.dead").increment();
            log.error("[BIZ] scrape.outbox.dead outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} reason=missing_job",
                    outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId());
            return;
        }

        try {
            String queueMessageId = scrapeJobPublisher.publish(outbox.getPayloadJson());
            outbox.markSent(queueMessageId, attemptedAt);
            meterRegistry.counter("scrape.outbox.publish.success").increment();
            log.info("[BIZ] scrape.outbox.sent outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={}",
                    outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), queueMessageId);
        } catch (RuntimeException e) {
            handleFailure(outbox, job, attemptedAt, e);
        }
    }

    private void handleFailure(ScrapeJobOutbox outbox, ScrapeJob job, Instant attemptedAt, RuntimeException exception) {
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
            log.error("[BIZ] scrape.outbox.dead outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} reason={}",
                    outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId(), summary);
            return;
        }

        Instant nextAttemptAt = attemptedAt.plusSeconds(calculateBackoffSeconds(outbox.getAttemptCount() + 1));
        outbox.markRetryableFailure(summary, attemptedAt, nextAttemptAt);
        meterRegistry.counter("scrape.outbox.retry").increment();
        log.warn("[BIZ] scrape.outbox.retry outboxId={} jobId={} attempt={} outboxStatus={} queueMessageId={} nextAttemptAt={} reason={}",
                outbox.getOutboxId(), outbox.getJobId(), outbox.getAttemptCount(), outbox.getStatus(), outbox.getQueueMessageId(), nextAttemptAt, summary);
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
}
