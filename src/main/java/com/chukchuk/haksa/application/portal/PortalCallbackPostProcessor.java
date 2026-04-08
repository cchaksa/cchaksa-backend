package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapper;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalCallbackPostProcessor {

    private final PortalSyncService portalSyncService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PortalCallbackPostProcessCommand command) {
        PortalData portalData;
        try {
            portalData = toPortalData(command.resultPayloadJson());
        } catch (JsonProcessingException e) {
            meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload").increment();
            log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} reason=invalid_payload message={}",
                    command.jobId(), command.userId(), command.operationType(), e.getOriginalMessage(), e);
            return;
        }

        String studentCode = portalData.student().studentCode();
        log.info("[BIZ] scrape.job.callback.postprocess.start jobId={} userId={} operationType={} studentCode={}",
                command.jobId(), command.userId(), command.operationType(), studentCode);
        try {
            if (command.operationType() == ScrapeJobOperationType.LINK) {
                portalSyncService.syncWithPortal(command.userId(), portalData);
            } else {
                portalSyncService.refreshFromPortal(command.userId(), portalData);
            }
            meterRegistry.counter("scrape.job.callback.postprocess.success").increment();
            log.info("[BIZ] scrape.job.callback.postprocess.success jobId={} userId={} operationType={} studentCode={}",
                    command.jobId(), command.userId(), command.operationType(), studentCode);
        } catch (EntityNotFoundException exception) {
            recordFailure(command, studentCode, "user_missing", exception);
        } catch (PortalScrapeException exception) {
            recordFailure(command, studentCode, "portal_conn_fail", exception);
        } catch (RuntimeException exception) {
            recordFailure(command, studentCode, "unexpected", exception);
        }
    }

    private void recordFailure(PortalCallbackPostProcessCommand command, String studentCode, String reason, Exception exception) {
        meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", reason).increment();
        log.error("[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} operationType={} studentCode={} reason={} message={}",
                command.jobId(), command.userId(), command.operationType(), studentCode, reason, exception.getMessage(), exception);
    }

    private PortalData toPortalData(String payloadJson) throws JsonProcessingException {
        RawPortalData rawPortalData = objectMapper.readValue(payloadJson, RawPortalData.class);
        return PortalDataMapper.toPortalData(rawPortalData);
    }
}
