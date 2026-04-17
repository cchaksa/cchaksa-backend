package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalLinkJobService {

    private final PortalLinkJobTxService portalLinkJobTxService;
    private final ScrapeJobOutboxDispatcher scrapeJobOutboxDispatcher;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public PortalLinkDto.AcceptedResponse acceptJob(UUID userId, String idempotencyKey, PortalLinkDto.LinkRequest request) {
        validateRequest(idempotencyKey, request);

        User user = userService.getUserById(userId);
        ScrapeJobOperationType operationType = Boolean.TRUE.equals(user.getPortalConnected())
                ? ScrapeJobOperationType.REFRESH
                : ScrapeJobOperationType.LINK;

        String requestFingerprint = createRequestFingerprint(request.portal_type(), request.username(), request.password(), operationType);
        String requestPayloadJson = toRequestPayloadJson(request.username(), request.password());
        Instant requestedAt = Instant.now();

        try {
            PortalLinkJobTxService.PreparedJob preparedJob = portalLinkJobTxService.createOrLoadJob(
                    userId,
                    idempotencyKey,
                    request.portal_type(),
                    operationType,
                    requestFingerprint,
                    requestPayloadJson,
                    request.username(),
                    request.password(),
                    requestedAt
            );
            return finalizeAcceptedJob(preparedJob, userId, request, operationType, idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            PortalLinkJobTxService.PreparedJob preparedJob =
                    portalLinkJobTxService.loadExistingJob(userId, idempotencyKey, requestFingerprint);
            return finalizeAcceptedJob(preparedJob, userId, request, operationType, idempotencyKey);
        } catch (CommonException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("[BIZ] scrape.job.enqueue.fail userId={} portalType={} idempotencyKey={}",
                    userId, request.portal_type(), idempotencyKey, exception);
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
        }
    }

    public static String createRequestFingerprint(String portalType, String username, String password, ScrapeJobOperationType operationType) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = String.join("|",
                    normalize(portalType),
                    username.trim(),
                    password,
                    operationType.name());
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create request fingerprint", exception);
        }
    }

    private static String normalize(String portalType) {
        return portalType == null ? "" : portalType.trim().toLowerCase();
    }

    private void validateRequest(String idempotencyKey, PortalLinkDto.LinkRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        if (request.username() == null || request.username().isBlank() || request.password() == null || request.password().isBlank()) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }
        if (!"suwon".equals(normalize(request.portal_type()))) {
            throw new CommonException(ErrorCode.UNSUPPORTED_PORTAL_TYPE);
        }
    }

    private String toRequestPayloadJson(String username, String password) {
        try {
            return objectMapper.writeValueAsString(new ScrapeJobMessage.RequestPayload(username, password));
        } catch (JsonProcessingException exception) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, exception);
        }
    }

    private PortalLinkDto.AcceptedResponse finalizeAcceptedJob(
            PortalLinkJobTxService.PreparedJob preparedJob,
            UUID userId,
            PortalLinkDto.LinkRequest request,
            ScrapeJobOperationType operationType,
            String idempotencyKey
    ) {
        if (preparedJob.dispatchRequired()) {
            dispatchSynchronously(preparedJob, request.portal_type(), idempotencyKey);
        }

        if (preparedJob.reused()) {
            log.info("[BIZ] scrape.job.idempotent.reuse jobId={} userId={} portalType={} operationType={} idempotencyKey={}",
                    preparedJob.jobId(), userId, request.portal_type(), operationType, idempotencyKey);
        } else {
            log.info("[BIZ] scrape.job.accepted jobId={} userId={} portalType={} operationType={} idempotencyKey={}",
                    preparedJob.jobId(), userId, request.portal_type(), operationType, idempotencyKey);
        }
        return new PortalLinkDto.AcceptedResponse(
                preparedJob.jobId(),
                "accepted",
                "/portal/link/jobs/" + preparedJob.jobId()
        );
    }

    private void dispatchSynchronously(
            PortalLinkJobTxService.PreparedJob preparedJob,
            String portalType,
            String idempotencyKey
    ) {
        try {
            scrapeJobOutboxDispatcher.dispatchOnce(preparedJob.outboxId());
            PortalLinkJobTxService.DispatchSnapshot snapshot = portalLinkJobTxService.loadDispatchSnapshot(preparedJob.outboxId());
            if (!snapshot.isSent()) {
                log.error("[BIZ] scrape.job.enqueue.sync.fail jobId={} outboxId={} portalType={} idempotencyKey={} jobStatus={} outboxStatus={} queueMessageId={} lastError={}",
                        snapshot.jobId(),
                        snapshot.outboxId(),
                        portalType,
                        idempotencyKey,
                        snapshot.jobStatus(),
                        snapshot.outboxStatus(),
                        snapshot.queueMessageId(),
                        snapshot.lastError());
                throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED);
            }
        } catch (CommonException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error("[BIZ] scrape.job.enqueue.sync.exception jobId={} outboxId={} portalType={} idempotencyKey={}",
                    preparedJob.jobId(), preparedJob.outboxId(), portalType, idempotencyKey, exception);
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
        }
    }
}
