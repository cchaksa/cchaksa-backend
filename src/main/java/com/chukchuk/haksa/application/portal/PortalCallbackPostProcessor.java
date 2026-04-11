package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
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

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class PortalCallbackPostProcessor {

    private static final String FAILED_POST_PROCESSING = "FAILED_POST_PROCESSING";
    private static final String FAILED_RESULT_SCHEMA = "FAILED_RESULT_SCHEMA";

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ScrapeResultCallbackTxService scrapeResultCallbackTxService;

    public PortalCallbackPostProcessor(
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            ScrapeResultCallbackTxService scrapeResultCallbackTxService
    ) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.scrapeResultCallbackTxService = scrapeResultCallbackTxService;
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
        long startedAt = System.nanoTime();
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
            scrapeResultCallbackTxService.completeSuccess(
                    jobId,
                    userId,
                    operationType,
                    portalData,
                    payloadJson,
                    finishedAt,
                    queuedAgeSeconds,
                    payloadHash
            );
            meterRegistry.counter("scrape.job.callback.postprocess.success").increment();
            long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
            meterRegistry.timer("scrape.job.callback.stage", "stage", "postprocess_tx")
                    .record(Duration.ofMillis(elapsedMs));
            log.info("[BIZ] scrape.job.callback.postprocess.success jobId={} userId={} operationType={} studentCode={} elapsed_ms={}",
                    jobId, userId, operationType, studentCode, elapsedMs);
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
        scrapeResultCallbackTxService.markFailed(
                jobId,
                finishedAt,
                queuedAgeSeconds,
                FAILED_RESULT_SCHEMA,
                exception.getOriginalMessage(),
                false
        );
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} reason=invalid_payload message={}",
                jobId, userId, operationType, exception.getOriginalMessage(), exception);
        throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID, exception);
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
        scrapeResultCallbackTxService.markFailed(
                jobId,
                finishedAt,
                queuedAgeSeconds,
                FAILED_POST_PROCESSING,
                failureDetail + ":" + exception.getMessage(),
                false
        );
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} operationType={} reason={} detail={}",
                jobId, operationType, reason, failureDetail, exception);
        throw new CommonException(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED, exception);
    }

    private PortalData toPortalData(String payloadJson) throws JsonProcessingException {
        RawPortalData rawPortalData = objectMapper.readValue(payloadJson, RawPortalData.class);
        return PortalDataMapper.toPortalData(rawPortalData);
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
}
