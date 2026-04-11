package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeResultCallbackTxService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final PortalSyncService portalSyncService;
    private final MeterRegistry meterRegistry;

    @Transactional
    public CallbackReceipt receiveSuccessCallback(
            String jobId,
            int attempt,
            String resultS3Key,
            String resultChecksum,
            String callbackMetadataJson,
            Instant receivedAt
    ) {
        ScrapeJob job = findJobForUpdate(jobId);
        if (job.hasProcessedAttempt(attempt) || job.isCompleted()) {
            return CallbackReceipt.duplicate(job);
        }

        job.markPostProcessing(resultS3Key, resultChecksum, callbackMetadataJson, attempt, receivedAt);
        return CallbackReceipt.accepted(job);
    }

    @Transactional
    public CallbackReceipt receiveFailedCallback(
            String jobId,
            int attempt,
            String callbackMetadataJson,
            String errorCode,
            String errorMessage,
            Boolean retryable,
            Instant receivedAt,
            Instant finishedAt
    ) {
        ScrapeJob job = findJobForUpdate(jobId);
        if (job.hasProcessedAttempt(attempt) || job.isCompleted()) {
            return CallbackReceipt.duplicate(job);
        }

        job.recordFailedCallback(
                attempt,
                receivedAt,
                callbackMetadataJson,
                errorCode,
                errorMessage,
                retryable,
                finishedAt
        );
        recordQueuedAge(job, finishedAt);
        return CallbackReceipt.accepted(job);
    }

    @Transactional
    public void completeSuccess(
            String jobId,
            UUID userId,
            ScrapeJobOperationType operationType,
            PortalData portalData,
            String payloadJson,
            Instant finishedAt,
            Double queuedAgeSeconds,
            String payloadHash
    ) {
        ScrapeJob job = findJobForUpdate(jobId);
        log.info("[BIZ] scrape.job.callback.postprocess.execute jobId={} currentStatus={}",
                job.getJobId(), job.getStatus());

        if (operationType == ScrapeJobOperationType.LINK) {
            portalSyncService.syncWithPortal(userId, portalData);
        } else {
            portalSyncService.refreshFromPortal(userId, portalData);
        }

        Instant resolvedFinishedAt = resolveFinishedAt(finishedAt);
        job.markSucceeded(payloadJson, resolvedFinishedAt);
        recordQueuedAge(job, finishedAt, queuedAgeSeconds);
        log.info("[BIZ] scrape.job.succeeded jobId={} operationType={} payloadHash={} finishedAt={}",
                job.getJobId(), job.getOperationType(), payloadHash, resolvedFinishedAt);
    }

    @Transactional
    public void markFailed(
            String jobId,
            Instant finishedAt,
            Double queuedAgeSeconds,
            String errorCode,
            String message,
            Boolean retryable
    ) {
        ScrapeJob job = findJobForUpdate(jobId);
        if (job.isCompleted() || job.getStatus() == ScrapeJobStatus.SUCCEEDED) {
            log.info("[BIZ] scrape.job.callback.fail.skip jobId={} status={} errorCode={}",
                    job.getJobId(), job.getStatus(), errorCode);
            return;
        }
        job.markFailed(errorCode, message, retryable, resolveFinishedAt(finishedAt));
        recordQueuedAge(job, finishedAt, queuedAgeSeconds);
    }

    private ScrapeJob findJobForUpdate(String jobId) {
        return scrapeJobRepository.findForUpdateByJobId(jobId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null || finishedAt == null) {
            return;
        }
        double queuedAgeSeconds = Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
        meterRegistry.summary("scrape.job.queued.age.seconds").record(queuedAgeSeconds);
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt, Double queuedAgeSeconds) {
        double value;
        if (queuedAgeSeconds != null) {
            value = queuedAgeSeconds;
        } else if (job.getCreatedAt() != null && finishedAt != null) {
            value = Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
        } else {
            return;
        }
        meterRegistry.summary("scrape.job.queued.age.seconds").record(value);
    }

    private static Instant resolveFinishedAt(Instant finishedAt) {
        return finishedAt != null ? finishedAt : Instant.now();
    }

    public record CallbackReceipt(
            boolean duplicate,
            String jobId,
            UUID userId,
            ScrapeJobOperationType operationType,
            String status,
            Double queuedAgeSeconds
    ) {
        public static CallbackReceipt accepted(ScrapeJob job) {
            return new CallbackReceipt(
                    false,
                    job.getJobId(),
                    job.getUserId(),
                    job.getOperationType(),
                    job.getStatus().name().toLowerCase(Locale.ROOT),
                    calculateQueuedAgeSeconds(job)
            );
        }

        public static CallbackReceipt duplicate(ScrapeJob job) {
            return new CallbackReceipt(
                    true,
                    job.getJobId(),
                    job.getUserId(),
                    job.getOperationType(),
                    job.getStatus().name().toLowerCase(Locale.ROOT),
                    calculateQueuedAgeSeconds(job)
            );
        }

        private static Double calculateQueuedAgeSeconds(ScrapeJob job) {
            if (job.getCreatedAt() == null) {
                return null;
            }
            return Duration.between(job.getCreatedAt(), Instant.now()).toMillis() / 1000.0;
        }
    }
}
