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
import org.springframework.data.domain.Pageable;

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

    @Test
    @DisplayName("publish 성공 시 outbox를 SENT로 전이한다")
    void dispatchEligibleOutboxes_marksSent() {
        ScrapingProperties properties = scrapingProperties();
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry()
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenReturn("msg-1");

        dispatcher.dispatchEligibleOutboxes();

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
                new SimpleMeterRegistry()
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(SdkClientException.create("temporary failure"));

        dispatcher.dispatchEligibleOutboxes();

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
                new SimpleMeterRegistry()
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetsForUpdate(any(), any(), any(Pageable.class))).thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(new IllegalStateException("missing queue-url"));

        dispatcher.dispatchEligibleOutboxes();

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.DEAD);
        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("SCRAPE_JOB_ENQUEUE_FAILED");
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
