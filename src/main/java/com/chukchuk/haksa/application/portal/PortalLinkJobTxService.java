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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortalLinkJobTxService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public PreparedJob createOrLoadJob(
            UUID userId,
            String idempotencyKey,
            String portalType,
            ScrapeJobOperationType operationType,
            String requestFingerprint,
            String requestPayloadJson,
            String username,
            String password,
            Instant requestedAt
    ) {
        return scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .map(existingJob -> toPreparedJob(existingJob, requestFingerprint))
                .orElseGet(() -> createNewJob(
                        userId,
                        idempotencyKey,
                        portalType,
                        operationType,
                        requestFingerprint,
                        requestPayloadJson,
                        username,
                        password,
                        requestedAt
                ));
    }

    @Transactional(readOnly = true)
    public PreparedJob loadExistingJob(UUID userId, String idempotencyKey, String requestFingerprint) {
        ScrapeJob existingJob = scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
        return toPreparedJob(existingJob, requestFingerprint);
    }

    @Transactional(readOnly = true)
    public DispatchSnapshot loadDispatchSnapshot(String outboxId) {
        ScrapeJobOutbox outbox = scrapeJobOutboxRepository.findById(outboxId)
                .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
        ScrapeJob job = scrapeJobRepository.findById(outbox.getJobId())
                .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
        return new DispatchSnapshot(
                job.getJobId(),
                outbox.getOutboxId(),
                job.getStatus(),
                outbox.getStatus(),
                outbox.getQueueMessageId(),
                outbox.getLastError()
        );
    }

    private PreparedJob createNewJob(
            UUID userId,
            String idempotencyKey,
            String portalType,
            ScrapeJobOperationType operationType,
            String requestFingerprint,
            String requestPayloadJson,
            String username,
            String password,
            Instant requestedAt
    ) {
        ScrapeJob savedJob = scrapeJobRepository.save(ScrapeJob.createQueued(
                userId,
                portalType,
                operationType,
                idempotencyKey,
                requestFingerprint,
                requestPayloadJson
        ));
        ScrapeJobOutbox savedOutbox = scrapeJobOutboxRepository.save(
                ScrapeJobOutbox.createPending(
                        savedJob.getJobId(),
                        buildOutboxPayload(savedJob.getJobId(), userId, portalType, username, password, requestedAt),
                        requestedAt
                )
        );
        return PreparedJob.created(savedJob, savedOutbox);
    }

    private String buildOutboxPayload(
            String jobId,
            UUID userId,
            String portalType,
            String username,
            String password,
            Instant requestedAt
    ) {
        try {
            return objectMapper.writeValueAsString(new ScrapeJobMessage(
                    jobId,
                    userId.toString(),
                    portalType,
                    new ScrapeJobMessage.RequestPayload(username, password),
                    requestedAt.toString()
            ));
        } catch (JsonProcessingException exception) {
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
        }
    }

    private PreparedJob toPreparedJob(ScrapeJob existingJob, String requestFingerprint) {
        if (!existingJob.hasSameFingerprint(requestFingerprint)) {
            throw new CommonException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        ScrapeJobOutbox outbox = scrapeJobOutboxRepository.findByJobId(existingJob.getJobId())
                .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
        if (existingJob.getStatus() == ScrapeJobStatus.QUEUED && outbox.getStatus() == ScrapeJobOutboxStatus.DEAD) {
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED);
        }
        return PreparedJob.loaded(existingJob, outbox, requiresDispatch(existingJob, outbox));
    }

    private boolean requiresDispatch(ScrapeJob job, ScrapeJobOutbox outbox) {
        return job.getStatus() == ScrapeJobStatus.QUEUED
                && (outbox.getStatus() == ScrapeJobOutboxStatus.PENDING
                || outbox.getStatus() == ScrapeJobOutboxStatus.RETRYABLE_FAILED);
    }

    public record PreparedJob(
            String jobId,
            String outboxId,
            boolean reused,
            boolean dispatchRequired
    ) {
        static PreparedJob created(ScrapeJob job, ScrapeJobOutbox outbox) {
            return new PreparedJob(job.getJobId(), outbox.getOutboxId(), false, true);
        }

        static PreparedJob loaded(ScrapeJob job, ScrapeJobOutbox outbox, boolean dispatchRequired) {
            return new PreparedJob(job.getJobId(), outbox.getOutboxId(), true, dispatchRequired);
        }
    }

    public record DispatchSnapshot(
            String jobId,
            String outboxId,
            ScrapeJobStatus jobStatus,
            ScrapeJobOutboxStatus outboxStatus,
            String queueMessageId,
            String lastError
    ) {
        public boolean isSent() {
            return outboxStatus == ScrapeJobOutboxStatus.SENT && jobStatus == ScrapeJobStatus.RUNNING;
        }
    }
}
