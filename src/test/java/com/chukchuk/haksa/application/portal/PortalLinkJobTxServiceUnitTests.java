package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalLinkJobTxServiceUnitTests {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ScrapeJobOutboxRepository scrapeJobOutboxRepository;

    @Test
    @DisplayName("새 job 생성 시 outbox payload에는 실제 job_id가 기록된다")
    void createOrLoadJob_createsJobAndOutboxPayload() throws Exception {
        UUID userId = UUID.randomUUID();
        PortalLinkJobTxService service = new PortalLinkJobTxService(scrapeJobRepository, scrapeJobOutboxRepository, new ObjectMapper().findAndRegisterModules());

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.empty());
        when(scrapeJobRepository.save(any(ScrapeJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(scrapeJobOutboxRepository.save(any(ScrapeJobOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortalLinkJobTxService.PreparedJob preparedJob = service.createOrLoadJob(
                userId,
                "idem-1",
                "suwon",
                ScrapeJobOperationType.LINK,
                "fingerprint",
                "{\"username\":\"17019013\",\"password\":\"pw\"}",
                "17019013",
                "pw",
                Instant.parse("2026-04-17T01:02:03Z")
        );

        assertThat(preparedJob.reused()).isFalse();
        assertThat(preparedJob.dispatchRequired()).isTrue();
        ArgumentCaptor<ScrapeJobOutbox> captor = ArgumentCaptor.forClass(ScrapeJobOutbox.class);
        verify(scrapeJobOutboxRepository).save(captor.capture());
        JsonNode payload = new ObjectMapper().readTree(captor.getValue().getPayloadJson());
        assertThat(payload.path("job_id").asText()).isEqualTo(preparedJob.jobId());
        assertThat(payload.path("requested_at").asText()).isEqualTo("2026-04-17T01:02:03Z");
    }

    @Test
    @DisplayName("기존 QUEUED + RETRYABLE_FAILED job은 같은 idempotency key 재요청 시 다시 publish 대상이 된다")
    void loadExistingJob_marksQueuedRetryableAsDispatchRequired() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobTxService service = new PortalLinkJobTxService(scrapeJobRepository, scrapeJobOutboxRepository, new ObjectMapper().findAndRegisterModules());
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{}"
        );
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{}", Instant.now());
        outbox.markRetryableFailure("temporary failure", Instant.now(), Instant.now().plusSeconds(5));

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.of(job));
        when(scrapeJobOutboxRepository.findByJobId(job.getJobId())).thenReturn(Optional.of(outbox));

        PortalLinkJobTxService.PreparedJob preparedJob = service.loadExistingJob(userId, "idem-1", "fingerprint");

        assertThat(preparedJob.reused()).isTrue();
        assertThat(preparedJob.dispatchRequired()).isTrue();
    }

    @Test
    @DisplayName("기존 fingerprint가 다르면 conflict를 유지한다")
    void loadExistingJob_throwsConflictWhenFingerprintDiffers() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobTxService service = new PortalLinkJobTxService(scrapeJobRepository, scrapeJobOutboxRepository, new ObjectMapper().findAndRegisterModules());
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{}"
        );

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> service.loadExistingJob(userId, "idem-1", "another"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.code()));
    }

    @Test
    @DisplayName("기존 QUEUED job의 outbox가 DEAD면 enqueue 실패로 처리한다")
    void loadExistingJob_throwsWhenDeadOutbox() {
        UUID userId = UUID.randomUUID();
        PortalLinkJobTxService service = new PortalLinkJobTxService(scrapeJobRepository, scrapeJobOutboxRepository, new ObjectMapper().findAndRegisterModules());
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                "suwon",
                ScrapeJobOperationType.LINK,
                "idem-1",
                "fingerprint",
                "{}"
        );
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{}", Instant.now());
        outbox.markDead("fatal", Instant.now());

        when(scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, "idem-1")).thenReturn(Optional.of(job));
        when(scrapeJobOutboxRepository.findByJobId(job.getJobId())).thenReturn(Optional.of(outbox));

        assertThatThrownBy(() -> service.loadExistingJob(userId, "idem-1", "fingerprint"))
                .isInstanceOf(CommonException.class)
                .satisfies(ex -> assertThat(((CommonException) ex).getCode()).isEqualTo(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED.code()));
    }

    @Test
    @DisplayName("dispatch snapshot은 job/outbox 현재 상태를 함께 반환한다")
    void loadDispatchSnapshot_returnsCurrentState() {
        PortalLinkJobTxService service = new PortalLinkJobTxService(scrapeJobRepository, scrapeJobOutboxRepository, new ObjectMapper().findAndRegisterModules());
        UUID userId = UUID.randomUUID();
        ScrapeJob job = ScrapeJob.createQueued(userId, "suwon", ScrapeJobOperationType.LINK, "idem-1", "fingerprint", "{}");
        job.markRunning();
        ScrapeJobOutbox outbox = ScrapeJobOutbox.createPending(job.getJobId(), "{}", Instant.now());
        outbox.markSent("msg-1", Instant.now());

        when(scrapeJobOutboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));
        when(scrapeJobRepository.findById(job.getJobId())).thenReturn(Optional.of(job));

        PortalLinkJobTxService.DispatchSnapshot snapshot = service.loadDispatchSnapshot("outbox-1");

        assertThat(snapshot.jobStatus()).isEqualTo(ScrapeJobStatus.RUNNING);
        assertThat(snapshot.outboxStatus()).isEqualTo(ScrapeJobOutboxStatus.SENT);
        assertThat(snapshot.isSent()).isTrue();
    }
}
