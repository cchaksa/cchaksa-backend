package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.dao.CannotAcquireLockException;

import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobOutboxAfterCommitExecutor {

    private final ScrapeJobOutboxDispatcher scrapeJobOutboxDispatcher;
    @Qualifier("scrapeOutboxDispatchExecutor")
    private final TaskExecutor scrapeOutboxDispatchExecutor;
    @Qualifier("scrapeOutboxRetryScheduler")
    private final TaskScheduler scrapeOutboxRetryScheduler;
    private final ScrapingProperties scrapingProperties;

    public void dispatchAsync(ScrapeJob job, ScrapeJobOutbox outbox) {
        submit(job, outbox, 0);
    }

    private void submit(ScrapeJob job, ScrapeJobOutbox outbox, int attempt) {
        scrapeOutboxDispatchExecutor.execute(() -> executeDispatch(job, outbox, attempt));
    }

    private void executeDispatch(ScrapeJob job, ScrapeJobOutbox outbox, int attempt) {
        log.info("[BIZ] scrape.outbox.dispatch.after_commit.start jobId={} outboxId={} portalType={} attempt={} idempotencyKey={}",
                job.getJobId(), outbox.getOutboxId(), job.getPortalType(), attempt, job.getIdempotencyKey());
        try {
            int dispatched = scrapeJobOutboxDispatcher.dispatchOnce(outbox.getOutboxId());
            log.info("[BIZ] scrape.outbox.dispatch.after_commit.end jobId={} outboxId={} portalType={} attempt={} dispatchedCount={} idempotencyKey={}",
                    job.getJobId(), outbox.getOutboxId(), job.getPortalType(), attempt, dispatched, job.getIdempotencyKey());
        } catch (CannotCreateTransactionException | CannotGetJdbcConnectionException | CannotAcquireLockException exception) {
            log.warn("[BIZ] scrape.outbox.dispatch.after_commit.retry jobId={} outboxId={} attempt={} reason={} idempotencyKey={}",
                    job.getJobId(), outbox.getOutboxId(), attempt, exception.getMessage(), job.getIdempotencyKey());
            scheduleRetry(job, outbox, attempt + 1);
        } catch (RuntimeException exception) {
            log.error("[BIZ] scrape.outbox.dispatch.after_commit.fail jobId={} outboxId={} portalType={} attempt={} idempotencyKey={}",
                    job.getJobId(), outbox.getOutboxId(), job.getPortalType(), attempt, job.getIdempotencyKey(), exception);
        }
    }

    private void scheduleRetry(ScrapeJob job, ScrapeJobOutbox outbox, int attempt) {
        int maxAttempts = scrapingProperties.getPublisher().getAfterCommit().getMaxAttempts();
        if (attempt >= maxAttempts) {
            log.error("[BIZ] scrape.outbox.dispatch.after_commit.give_up jobId={} outboxId={} idempotencyKey={} attempts={}",
                    job.getJobId(), outbox.getOutboxId(), job.getIdempotencyKey(), attempt);
            return;
        }

        long backoff = calculateBackoffMillis(attempt);
        Instant nextAttempt = Instant.now().plusMillis(backoff);
        log.info("[BIZ] scrape.outbox.dispatch.after_commit.retry_schedule jobId={} outboxId={} attempt={} nextAttempt={} delayMs={} idempotencyKey={}",
                job.getJobId(), outbox.getOutboxId(), attempt, nextAttempt, backoff, job.getIdempotencyKey());
        scrapeOutboxRetryScheduler.schedule(() -> submit(job, outbox, attempt), Date.from(nextAttempt));
    }

    private long calculateBackoffMillis(int attempt) {
        long initialDelay = scrapingProperties.getPublisher().getAfterCommit().getInitialDelayMs();
        long maxDelay = scrapingProperties.getPublisher().getAfterCommit().getMaxDelayMs();
        long calculated = initialDelay * (1L << Math.max(0, attempt - 1));
        return Math.min(calculated, maxDelay);
    }
}
