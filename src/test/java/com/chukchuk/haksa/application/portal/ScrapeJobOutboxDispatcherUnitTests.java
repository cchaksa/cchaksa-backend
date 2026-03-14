package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class ScrapeJobOutboxDispatcherUnitTests {

    @Mock
    private ScrapeJobOutboxRepository scrapeJobOutboxRepository;

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ScrapeJobPublisher scrapeJobPublisher;

    @Mock
    private TransactionOperations transactionOperations;

    @Mock
    private Environment environment;

    @Test
    @DisplayName("publish 성공 시 outbox를 SENT로 전이한다")
    void dispatchEligibleOutboxes_marksSent() {
        ScrapingProperties properties = scrapingProperties();
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                transactionOperations,
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenReturn("msg-1");
        when(transactionOperations.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        dispatcher.dispatchOnce(outbox.getOutboxId());

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.SENT);
        assertThat(outbox.getQueueMessageId()).isEqualTo("msg-1");
    }

    @Test
    @DisplayName("일시적 publish 실패 시 outbox를 RETRYABLE_FAILED로 전이한다")
    void dispatchEligibleOutboxes_marksRetryableFailed() {
        ScrapingProperties properties = scrapingProperties();
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                transactionOperations,
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(SdkClientException.create("temporary failure"));
        when(transactionOperations.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        dispatcher.dispatchOnce(outbox.getOutboxId());

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.RETRYABLE_FAILED);
        assertThat(outbox.getNextAttemptAt()).isNotNull();
    }

    @Test
    @DisplayName("설정 오류나 최대 재시도 초과 시 outbox와 job을 DEAD/FAILED로 확정한다")
    void dispatchEligibleOutboxes_marksDeadAndJobFailed() {
        ScrapingProperties properties = scrapingProperties();
        properties.getPublisher().setMaxAttempts(1);
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                transactionOperations,
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(new IllegalStateException("missing queue-url"));
        when(transactionOperations.execute(any())).thenAnswer(invocation -> invocation.getArgument(0, org.springframework.transaction.support.TransactionCallback.class).doInTransaction(null));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        dispatcher.dispatchOnce(outbox.getOutboxId());

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.DEAD);
        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("SCRAPE_JOB_ENQUEUE_FAILED");
    }

    @Test
    @DisplayName("connection 획득 실패는 one-shot dispatch 예외로 노출한다")
    void dispatchOnce_throwsWhenConnectionAcquisitionFails() {
        ScrapingProperties properties = scrapingProperties();
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                transactionOperations,
                environment
        );

        when(transactionOperations.execute(any())).thenThrow(new org.springframework.transaction.CannotCreateTransactionException("db down"));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop-shadow"});

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> dispatcher.dispatchOnce("outbox-1"))
                .isInstanceOf(org.springframework.transaction.CannotCreateTransactionException.class);
    }

    private static ScrapingProperties scrapingProperties() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.getPublisher().setEnabled(true);
        properties.getPublisher().setBatchSize(10);
        properties.getPublisher().setMaxAttempts(3);
        properties.getPublisher().setInitialBackoffSeconds(1);
        properties.getPublisher().setMaxBackoffSeconds(10);
        return properties;
    }

    private static ScrapeJob queuedJob() {
        return ScrapeJob.createQueued(
                UUID.randomUUID(),
                "suwon",
                com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
    }
}
