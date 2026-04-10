package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapper;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class PortalCallbackPostProcessor {

    private static final String FAILED_POST_PROCESSING = "FAILED_POST_PROCESSING";
    private static final String FAILED_RESULT_SCHEMA = "FAILED_RESULT_SCHEMA";

    private final PortalSyncService portalSyncService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ScrapeJobRepository scrapeJobRepository;
    private final TransactionTemplate successTemplate;

    public PortalCallbackPostProcessor(
            PortalSyncService portalSyncService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            ScrapeJobRepository scrapeJobRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.portalSyncService = portalSyncService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.scrapeJobRepository = scrapeJobRepository;
        this.successTemplate = buildRequiresNewTemplate(transactionManager);
    }

    public void process(
            String jobId,
            UUID userId,
            ScrapeJobOperationType operationType,
            String payloadJson,
            Instant finishedAt,
            Double queuedAgeSeconds,
            int attempt,
            String workerRequestId,
            String payloadHash
    ) {
        PortalData portalData;
        try {
            portalData = toPortalData(payloadJson);
        } catch (JsonProcessingException e) {
            handleParsingFailure(jobId, userId, operationType, finishedAt, queuedAgeSeconds, e);
            return;
        }

        String studentCode = portalData.student().studentCode();
        log.info("[BIZ] scrape.job.callback.postprocess.start jobId={} userId={} operationType={} studentCode={} attempt={} requestId={} payloadHash={}",
                jobId, userId, operationType, studentCode, attempt, workerRequestId, payloadHash);
        try {
            successTemplate.executeWithoutResult(status -> {
                ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(jobId)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));
                log.info("[BIZ] scrape.job.callback.postprocess.execute jobId={} currentStatus={}", job.getJobId(), job.getStatus());
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
            });
            meterRegistry.counter("scrape.job.callback.postprocess.success").increment();
            log.info("[BIZ] scrape.job.callback.postprocess.success jobId={} userId={} operationType={} studentCode={}",
                    jobId, userId, operationType, studentCode);
        } catch (EntityNotFoundException exception) {
            recordFailure(jobId, finishedAt, queuedAgeSeconds, "user_missing", operationType, exception);
        } catch (PortalScrapeException exception) {
            recordFailure(jobId, finishedAt, queuedAgeSeconds, "portal_conn_fail", operationType, exception);
        } catch (DataIntegrityViolationException exception) {
            recordFailure(jobId, finishedAt, queuedAgeSeconds, "data_integrity", operationType, exception);
        } catch (RuntimeException exception) {
            recordFailure(jobId, finishedAt, queuedAgeSeconds, "unexpected", operationType, exception);
        }
    }

    private void handleParsingFailure(
            String jobId,
            UUID userId,
            ScrapeJobOperationType operationType,
            Instant finishedAt,
            Double queuedAgeSeconds,
            JsonProcessingException exception
    ) {
        meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload").increment();
        markJobFailed(jobId, finishedAt, queuedAgeSeconds, FAILED_RESULT_SCHEMA, exception.getOriginalMessage());
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} reason=invalid_payload message={}",
                jobId, userId, operationType, exception.getOriginalMessage(), exception);
    }

    private void recordFailure(
            String jobId,
            Instant finishedAt,
            Double queuedAgeSeconds,
            String reason,
            ScrapeJobOperationType operationType,
            Exception exception
    ) {
        meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", reason).increment();
        String failureDetail = failureCode(reason, exception, operationType);
        markJobFailed(jobId, finishedAt, queuedAgeSeconds, FAILED_POST_PROCESSING, failureDetail + ":" + exception.getMessage());
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} operationType={} reason={} detail={}",
                jobId, operationType, reason, failureDetail, exception);
    }

    private void markJobFailed(String jobId, Instant finishedAt, Double queuedAgeSeconds, String errorCode, String message) {
        successTemplate.executeWithoutResult(status -> {
            ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(jobId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));
            job.markFailed(errorCode, message, false, resolveFinishedAt(finishedAt));
            recordQueuedAge(job, finishedAt, queuedAgeSeconds);
        });
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

    private PortalData toPortalData(String payloadJson) throws JsonProcessingException {
        RawPortalData rawPortalData = objectMapper.readValue(payloadJson, RawPortalData.class);
        return PortalDataMapper.toPortalData(rawPortalData);
    }

    private static Instant resolveFinishedAt(Instant finishedAt) {
        return finishedAt != null ? finishedAt : Instant.now();
    }

    private String failureCode(String reason, Exception exception, ScrapeJobOperationType operationType) {
        if (exception instanceof EntityNotFoundException entityNotFoundException) {
            return entityNotFoundException.getCode();
        }
        if (exception instanceof PortalScrapeException portalScrapeException) {
            return portalScrapeException.getCode();
        }
        if ("data_integrity".equals(reason)) {
            return "DATA_INTEGRITY_VIOLATION";
        }
        if ("invalid_payload".equals(reason)) {
            return "INVALID_PORTAL_PAYLOAD";
        }
        if ("user_missing".equals(reason)) {
            return ErrorCode.USER_NOT_FOUND.code();
        }
        if ("portal_conn_fail".equals(reason)) {
            return operationType == ScrapeJobOperationType.LINK
                    ? ErrorCode.SCRAPING_FAILED.code()
                    : ErrorCode.REFRESH_FAILED.code();
        }
        return "PORTAL_CALLBACK_POSTPROCESS_ERROR";
    }

    private static TransactionTemplate buildRequiresNewTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
