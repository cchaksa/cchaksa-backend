package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScrapeJobStaleReconcilerUnitTests {

    @Mock
    private ScrapeJobOutboxRepository scrapeJobOutboxRepository;

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Test
    @DisplayName("SENT 이후 callback이 늦으면 job을 FAILED로 확정한다")
    void reconcileStaleQueuedJobs_marksJobFailed() {
        ScrapingProperties properties = new ScrapingProperties();
        properties.getStale().setEnabled(true);
        properties.getStale().setBatchSize(10);
        properties.getStale().setTimeoutSeconds(60);

        ScrapeJobStaleReconciler reconciler = new ScrapeJobStaleReconciler(
                scrapeJobOutboxRepository,
                scrapeJobRepository,
                properties,
                new SimpleMeterRegistry()
        );

        ScrapeJob job = ScrapeJob.createQueued(
                UUID.randomUUID(),
                "suwon",
                ScrapeJobOperationType.REFRESH,
                "idem-1",
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}"
        );
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{\"job_id\":\"" + job.getJobId() + "\"}", Instant.now());
        job.markRunning();
        outbox.markSent("msg-1", Instant.now().minusSeconds(120));

        when(scrapeJobOutboxRepository.findStaleSentTargetsForUpdate(eq(ScrapeJobOutboxStatus.SENT), any(), eq(ScrapeJobStatus.RUNNING), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));

        int affectedCount = reconciler.reconcileStaleQueuedJobs();

        assertThat(affectedCount).isEqualTo(1);
        assertThat(job.getStatus().name()).isEqualTo("FAILED");
        assertThat(job.getErrorCode()).isEqualTo("CALLBACK_TIMEOUT");
        assertThat(job.getRetryable()).isTrue();
    }
}
