package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapeJobOutboxAfterCommitExecutorTests {

    @Mock
    private ScrapeJobOutboxDispatcher scrapeJobOutboxDispatcher;

    @Test
    @DisplayName("afterCommit executor는 성공 시 dispatcher를 한 번만 호출한다")
    void dispatchAsync_executesOnceOnSuccess() {
        ScrapingProperties properties = properties();
        ImmediateTaskExecutor executor = new ImmediateTaskExecutor();
        RecordingTaskScheduler scheduler = new RecordingTaskScheduler();
        ScrapeJobOutboxAfterCommitExecutor afterCommitExecutor = new ScrapeJobOutboxAfterCommitExecutor(
                scrapeJobOutboxDispatcher,
                executor,
                scheduler,
                properties
        );
        ScrapeJob job = ScrapeJob.createQueued(UUID.randomUUID(), "suwon", com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType.LINK, "idem-1", "fp", "{}");
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{}", Instant.now());

        when(scrapeJobOutboxDispatcher.dispatchOnce(outbox.getOutboxId())).thenReturn(1);

        afterCommitExecutor.dispatchAsync(job, outbox);

        verify(scrapeJobOutboxDispatcher).dispatchOnce(outbox.getOutboxId());
        scheduler.assertNoScheduledRetry();
    }

    @Test
    @DisplayName("커넥션 오류 시 재시도 작업을 예약한다")
    void dispatchAsync_retriesWhenConnectionUnavailable() {
        ScrapingProperties properties = properties();
        properties.getPublisher().getAfterCommit().setMaxAttempts(3);
        ImmediateTaskExecutor executor = new ImmediateTaskExecutor();
        RecordingTaskScheduler scheduler = new RecordingTaskScheduler();
        ScrapeJobOutboxAfterCommitExecutor afterCommitExecutor = new ScrapeJobOutboxAfterCommitExecutor(
                scrapeJobOutboxDispatcher,
                executor,
                scheduler,
                properties
        );
        ScrapeJob job = ScrapeJob.createQueued(UUID.randomUUID(), "suwon", com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType.LINK, "idem-1", "fp", "{}");
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{}", Instant.now());

        when(scrapeJobOutboxDispatcher.dispatchOnce(outbox.getOutboxId()))
                .thenThrow(new CannotCreateTransactionException("pool exhausted", null))
                .thenReturn(1);

        afterCommitExecutor.dispatchAsync(job, outbox);
        scheduler.runNext();

        verify(scrapeJobOutboxDispatcher, times(2)).dispatchOnce(eq(outbox.getOutboxId()));
    }

    private static ScrapingProperties properties() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.getPublisher().getAfterCommit().setExecutorCorePoolSize(1);
        properties.getPublisher().getAfterCommit().setExecutorMaxPoolSize(1);
        properties.getPublisher().getAfterCommit().setQueueCapacity(10);
        properties.getPublisher().getAfterCommit().setInitialDelayMs(10);
        properties.getPublisher().getAfterCommit().setMaxDelayMs(100);
        return properties;
    }

    private static class ImmediateTaskExecutor implements TaskExecutor {
        @Override
        public void execute(Runnable task) {
            task.run();
        }
    }

    private static class RecordingTaskScheduler implements TaskScheduler {
        private final Queue<Runnable> scheduled = new ArrayDeque<>();

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            scheduled.add(task);
            return new NoOpScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            scheduled.add(task);
            return new NoOpScheduledFuture();
        }

        void runNext() {
            Runnable task = scheduled.poll();
            if (task != null) {
                task.run();
            }
        }

        void assertNoScheduledRetry() {
            if (!scheduled.isEmpty()) {
                throw new AssertionError("Unexpected scheduled retry");
            }
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            throw new UnsupportedOperationException();
        }
    }

    private static class NoOpScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed o) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
