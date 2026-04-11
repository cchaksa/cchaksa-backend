package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(eq(outbox.getOutboxId()), any(), any())).thenReturn(Optional.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenReturn("msg-1");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        dispatcher.dispatchOnce(outbox.getOutboxId());

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.SENT);
        assertThat(outbox.getQueueMessageId()).isEqualTo("msg-1");
        assertThat(job.getStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
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
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(eq(outbox.getOutboxId()), any(), any())).thenReturn(Optional.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(SdkClientException.create("temporary failure"));
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
                environment
        );
        ScrapeJob job = queuedJob();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());

        when(scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(eq(outbox.getOutboxId()), any(), any())).thenReturn(Optional.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
        when(scrapeJobPublisher.publish(outbox.getPayloadJson())).thenThrow(new IllegalStateException("missing queue-url"));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        dispatcher.dispatchOnce(outbox.getOutboxId());

        assertThat(outbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.DEAD);
        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("SCRAPE_JOB_ENQUEUE_FAILED");
    }

    @Test
    @DisplayName("조회 예외는 one-shot dispatch 예외로 노출한다")
    void dispatchOnce_throwsWhenLookupFails() {
        ScrapingProperties properties = scrapingProperties();
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                environment
        );

        when(scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(eq("outbox-1"), any(), any()))
                .thenThrow(new org.springframework.dao.InvalidDataAccessApiUsageException("tx required"));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop-shadow"});

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> dispatcher.dispatchOnce("outbox-1"))
                .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class);
    }

    @Test
    @DisplayName("preferred outbox는 배치에 없어도 직접 조회해 즉시 publish 한다")
    void dispatchOnce_publishesPreferredOutboxEvenWhenNotInBatchWindow() {
        ScrapingProperties properties = scrapingProperties();
        properties.getPublisher().setBatchSize(1);
        ScrapeJobOutboxDispatcher dispatcher = new ScrapeJobOutboxDispatcher(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                scrapeJobPublisher,
                properties,
                new SimpleMeterRegistry(),
                environment
        );
        ScrapeJob preferredJob = queuedJob();
        ScrapeJobOutbox preferredOutbox = ScrapeJobOutbox.createPending(
                preferredJob.getJobId(),
                "{\"job_id\":\"" + preferredJob.getJobId() + "\"}",
                Instant.now()
        );
        ScrapeJob otherJob = queuedJob();
        ScrapeJobOutbox otherOutbox = ScrapeJobOutbox.createPending(
                otherJob.getJobId(),
                "{\"job_id\":\"" + otherJob.getJobId() + "\"}",
                Instant.now()
        );

        when(scrapeJobOutboxRepository.findPublishTargetForUpdateByOutboxId(eq(preferredOutbox.getOutboxId()), any(), any()))
                .thenReturn(Optional.of(preferredOutbox));
        when(scrapeJobRepository.findForUpdateByJobId(preferredJob.getJobId())).thenReturn(Optional.of(preferredJob));
        when(scrapeJobPublisher.publish(preferredOutbox.getPayloadJson())).thenReturn("msg-preferred");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop-shadow"});

        int dispatchedCount = dispatcher.dispatchOnce(preferredOutbox.getOutboxId());

        assertThat(dispatchedCount).isEqualTo(1);
        assertThat(preferredOutbox.getStatus()).isEqualTo(ScrapeJobOutboxStatus.SENT);
        verify(scrapeJobPublisher).publish(preferredOutbox.getPayloadJson());
        verify(scrapeJobRepository, never()).findForUpdateByJobId(otherJob.getJobId());
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
