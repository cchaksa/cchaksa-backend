package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PortalLinkJobService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final UserService userService;
    private final ScrapeJobPublisher scrapeJobPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public PortalLinkJobService(
            ScrapeJobRepository scrapeJobRepository,
            UserService userService,
            @Qualifier("requestScrapeJobPublisher") ScrapeJobPublisher scrapeJobPublisher
    ) {
        this.scrapeJobRepository = scrapeJobRepository;
        this.userService = userService;
        this.scrapeJobPublisher = scrapeJobPublisher;
    }

    @Transactional
    public PortalLinkDto.AcceptedResponse acceptJob(UUID userId, String idempotencyKey, PortalLinkDto.LinkRequest request) {
        validateRequest(idempotencyKey, request);

        Optional<ScrapeJob> existingJob = scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingJob.isPresent()) {
            return reuseExistingJob(existingJob.get(), request);
        }

        User user = userService.getUserById(userId);
        ScrapeJobOperationType operationType = Boolean.TRUE.equals(user.getPortalConnected())
                ? ScrapeJobOperationType.REFRESH
                : ScrapeJobOperationType.LINK;

        String requestFingerprint = createRequestFingerprint(request.portal_type(), request.username(), request.password(), operationType);
        String requestPayloadJson = toRequestPayloadJson(request.username(), request.password());
        ScrapeJob job = ScrapeJob.createQueued(
                userId,
                request.portal_type(),
                operationType,
                idempotencyKey,
                requestFingerprint,
                requestPayloadJson
        );
        Instant requestedAt = Instant.now();

        try {
            ScrapeJob savedJob = scrapeJobRepository.save(job);
            enqueueScrapeJob(savedJob, userId, request, requestedAt);
            log.info("[BIZ] scrape.job.accepted jobId={} userId={} portalType={} operationType={} idempotencyKey={}",
                    savedJob.getJobId(), userId, request.portal_type(), operationType, idempotencyKey);
            return toAcceptedResponse(savedJob);
        } catch (DataIntegrityViolationException e) {
            return resolveConcurrentDuplicate(userId, request, idempotencyKey);
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
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create request fingerprint", e);
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

    private PortalLinkDto.AcceptedResponse reuseExistingJob(ScrapeJob existingJob, PortalLinkDto.LinkRequest request) {
        String requestFingerprint = createRequestFingerprint(
                request.portal_type(),
                request.username(),
                request.password(),
                existingJob.getOperationType()
        );

        if (!existingJob.hasSameFingerprint(requestFingerprint)) {
            throw new CommonException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        log.info("[BIZ] scrape.job.idempotent.reuse jobId={} userId={} portalType={} operationType={} idempotencyKey={}",
                existingJob.getJobId(), existingJob.getUserId(), existingJob.getPortalType(), existingJob.getOperationType(), existingJob.getIdempotencyKey());
        return toAcceptedResponse(existingJob);
    }

    private String toRequestPayloadJson(String username, String password) {
        try {
            return objectMapper.writeValueAsString(new ScrapeJobMessage.RequestPayload(username, password));
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private String toJobPayloadJson(ScrapeJobMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, e);
        }
    }

    private PortalLinkDto.AcceptedResponse resolveConcurrentDuplicate(
            UUID userId,
            PortalLinkDto.LinkRequest request,
            String idempotencyKey
    ) {
        ScrapeJob concurrentJob = scrapeJobRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
        return reuseExistingJob(concurrentJob, request);
    }

    private PortalLinkDto.AcceptedResponse toAcceptedResponse(ScrapeJob job) {
        return new PortalLinkDto.AcceptedResponse(
                job.getJobId(),
                "accepted",
                "/portal/link/jobs/" + job.getJobId()
        );
    }

    private void enqueueScrapeJob(ScrapeJob job, UUID userId, PortalLinkDto.LinkRequest request, Instant requestedAt) {
        ScrapeJobMessage message = new ScrapeJobMessage(
                job.getJobId(),
                userId.toString(),
                request.portal_type(),
                new ScrapeJobMessage.RequestPayload(request.username(), request.password()),
                requestedAt.toString(),
                userId.toString(),
                job.getJobId()
        );

        String payloadJson = toJobPayloadJson(message);
        try {
            String queueMessageId = scrapeJobPublisher.publish(
                    payloadJson,
                    message.message_group_id(),
                    message.message_deduplication_id()
            );
            log.info("[BIZ] scrape.job.publish.success jobId={} queueMessageId={} groupId={} dedupId={} idempotencyKey={}",
                    job.getJobId(), queueMessageId, message.message_group_id(), message.message_deduplication_id(), job.getIdempotencyKey());
        } catch (RuntimeException e) {
            log.error("[BIZ] scrape.job.publish.fail jobId={} idempotencyKey={}",
                    job.getJobId(), job.getIdempotencyKey(), e);
            throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, e);
        }
    }
}
