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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class PortalCallbackPostProcessor {

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("portalCallbackExecutor")
    public void handle(PortalCallbackPostProcessCommand command) {
        PortalData portalData;
        try {
            portalData = toPortalData(command.resultPayloadJson());
        } catch (JsonProcessingException e) {
            handleParsingFailure(command, e);
            return;
        }

        String studentCode = portalData.student().studentCode();
        log.info("[BIZ] scrape.job.callback.postprocess.start jobId={} userId={} operationType={} studentCode={} attempt={} requestId={} payloadHash={}",
                command.jobId(), command.userId(), command.operationType(), studentCode, command.attempt(), command.workerRequestId(), command.payloadHash());
        try {
            successTemplate.executeWithoutResult(status -> {
                ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(command.jobId())
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND));
                log.info("[BIZ] scrape.job.callback.postprocess.execute jobId={} currentStatus={}", job.getJobId(), job.getStatus());
                if (command.operationType() == ScrapeJobOperationType.LINK) {
                    portalSyncService.syncWithPortal(command.userId(), portalData);
                } else {
                    portalSyncService.refreshFromPortal(command.userId(), portalData);
                }
                job.markSucceeded(command.resultPayloadJson(), resolveFinishedAt(command));
                recordQueuedAge(job, command.finishedAt(), command.queuedAgeSeconds());
            });
            meterRegistry.counter("scrape.job.callback.postprocess.success").increment();
            log.info("[BIZ] scrape.job.callback.postprocess.success jobId={} userId={} operationType={} studentCode={}",
                    command.jobId(), command.userId(), command.operationType(), studentCode);
        } catch (EntityNotFoundException exception) {
            recordFailure(command, studentCode, "user_missing", exception);
        } catch (PortalScrapeException exception) {
            recordFailure(command, studentCode, "portal_conn_fail", exception);
        } catch (DataIntegrityViolationException exception) {
            recordFailure(command, studentCode, "data_integrity", exception);
        } catch (RuntimeException exception) {
            recordFailure(command, studentCode, "unexpected", exception);
        }
    }

    private void handleParsingFailure(PortalCallbackPostProcessCommand command, JsonProcessingException exception) {
        meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload").increment();
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} reason=invalid_payload message={}",
                command.jobId(), command.userId(), command.operationType(), exception.getOriginalMessage(), exception);
    }

    private void recordFailure(PortalCallbackPostProcessCommand command, String studentCode, String reason, Exception exception) {
        meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", reason).increment();
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} studentCode={} reason={} message={}",
                command.jobId(), command.userId(), command.operationType(), studentCode, reason, exception.getMessage(), exception);
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

    private static Instant resolveFinishedAt(PortalCallbackPostProcessCommand command) {
        return command.finishedAt() != null ? command.finishedAt() : Instant.now();
    }

    private static TransactionTemplate buildRequiresNewTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
