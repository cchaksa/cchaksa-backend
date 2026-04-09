package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.BaseException;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapper;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeResultCallbackService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HmacSignatureVerifier hmacSignatureVerifier;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public void handleCallback(String rawBody, String timestamp, String signature, String attemptHeader, String workerRequestId) {
        String bodyHash = hashRawBody(rawBody);
        String hintedJobId = extractJobId(rawBody);
        int attempt = parseAttempt(attemptHeader);
        String normalizedWorkerRequestId = normalizeWorkerRequestId(workerRequestId);

        try {
            hmacSignatureVerifier.verify(timestamp, rawBody, signature);
        } catch (CommonException exception) {
            HmacSignatureVerifier.VerificationDiagnostics diagnostics = hmacSignatureVerifier.diagnostics(timestamp, rawBody, signature);
            log.warn("[BIZ] scrape.job.callback.invalid_signature jobId={} signatureValid=false reason={} timestamp={} parsedTimestamp={} timestampDeltaSeconds={} rawBodyHash={} actualSignatureEncoding={} actualSignatureLength={} actualSignatureHash={} expectedUtf8SignatureHash={} expectedHexSignatureHash={}",
                    hintedJobId,
                    diagnostics.reason(),
                    timestamp,
                    diagnostics.parsedTimestamp(),
                    diagnostics.timestampDeltaSeconds(),
                    bodyHash,
                    diagnostics.actualSignatureEncoding(),
                    diagnostics.actualSignatureLength(),
                    diagnostics.actualSignatureHash(),
                    diagnostics.expectedUtf8SignatureHash(),
                    diagnostics.expectedHexSignatureHash());
            throw exception;
        }

        PortalLinkDto.ScrapeResultCallbackRequest request = parseRequest(rawBody, bodyHash);
        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(request.job_id())
                .orElseThrow(() -> {
                    log.warn("[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
                            request.job_id(), bodyHash);
                    return new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND);
                });

        log.info("[BIZ] scrape.job.callback.received jobId={} status={} attempt={} requestId={} rawBodyHash={}",
                job.getJobId(), job.getStatus(), attempt, normalizedWorkerRequestId, bodyHash);

        if (job.isCompleted() || job.hasWorkerResult()) {
            handleDuplicate(job, attempt, normalizedWorkerRequestId);
            return;
        }

        Instant finishedAt = request.finished_at() == null ? Instant.now() : request.finished_at();
        String normalizedStatus = normalize(request.status());

        if ("succeeded".equals(normalizedStatus)) {
            handleSucceeded(job, request, finishedAt, bodyHash, attempt, normalizedWorkerRequestId);
            return;
        }

        if ("failed".equals(normalizedStatus)) {
            job.markFailed(request.error_code(), request.error_message(), request.retryable(), finishedAt);
            recordQueuedAge(job, finishedAt);
            log.info("[BIZ] scrape.job.failed jobId={} errorCode={} retryable={} attempt={} requestId={}",
                    job.getJobId(), request.error_code(), request.retryable(), attempt, normalizedWorkerRequestId);
            return;
        }

        throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }

    private PortalLinkDto.ScrapeResultCallbackRequest parseRequest(String rawBody, String bodyHash) {
        try {
            return objectMapper.readValue(rawBody, PortalLinkDto.ScrapeResultCallbackRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("[BIZ] scrape.job.callback.invalid_payload stage=request_parse rawBodyHash={} message={}",
                    bodyHash, e.getOriginalMessage());
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private void handleSucceeded(
            ScrapeJob job,
            PortalLinkDto.ScrapeResultCallbackRequest request,
            Instant finishedAt,
            String bodyHash,
            int attempt,
            String workerRequestId
    ) {
        try {
            if (request.result_payload() == null || request.result_payload().isNull()) {
                log.warn("[BIZ] scrape.job.callback.invalid_payload stage=result_payload_missing jobId={} rawBodyHash={}",
                        job.getJobId(), bodyHash);
                throw new CommonException(ErrorCode.INVALID_ARGUMENT);
            }
            JsonNode normalizedPayload = normalizeNodeKeys(request.result_payload());
            RawPortalData rawPortalData = objectMapper.treeToValue(normalizedPayload, RawPortalData.class);
            PortalDataMapper.toPortalData(rawPortalData); // validate payload before persisting

            String payloadJson = writeJson(normalizedPayload);
            String payloadHash = hashRawBody(payloadJson);
            job.recordWorkerResult(payloadJson, finishedAt);
            job.markPostProcessing();
            Double queuedAgeSeconds = calculateQueuedAgeSeconds(job, finishedAt);
            recordQueuedAge(job, finishedAt);
            meterRegistry.counter("scrape.job.callback.persisted").increment();
            log.info("[BIZ] scrape.job.callback.persisted jobId={} attempt={} requestId={} payloadHash={} queuedAgeSeconds={}",
                    job.getJobId(), attempt, workerRequestId, payloadHash, queuedAgeSeconds);
            eventPublisher.publishEvent(new PortalCallbackPostProcessCommand(
                    job.getJobId(),
                    job.getUserId(),
                    job.getOperationType(),
                    payloadJson,
                    finishedAt,
                    queuedAgeSeconds,
                    attempt,
                    workerRequestId,
                    payloadHash
            ));
        } catch (BaseException | IllegalArgumentException e) {
            job.markFailed("BUSINESS_RULE_VIOLATION", e.getMessage(), false, finishedAt);
            recordQueuedAge(job, finishedAt);
            log.warn("[BIZ] scrape.job.business_fail jobId={} operationType={} message={}",
                    job.getJobId(), job.getOperationType(), e.getMessage());
        } catch (JsonProcessingException e) {
            log.warn("[BIZ] scrape.job.callback.invalid_payload stage=result_payload_mapping jobId={} rawBodyHash={} resultPayloadKeys={} message={}",
                    job.getJobId(), bodyHash, topLevelFieldNames(request.result_payload()), e.getOriginalMessage());
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        } catch (RuntimeException e) {
            job.markFailed("INTERNAL_ERROR", e.getMessage(), false, finishedAt);
            recordQueuedAge(job, finishedAt);
            log.error("[BIZ] scrape.job.callback.unexpected_fail jobId={} operationType={} ex={}",
                    job.getJobId(), job.getOperationType(), e.getClass().getSimpleName(), e);
        }
    }

    private void handleDuplicate(ScrapeJob job, int attempt, String workerRequestId) {
        meterRegistry.counter("scrape.job.callback.duplicate", "status", job.getStatus().name().toLowerCase(Locale.ROOT))
                .increment();
        log.info("[BIZ] scrape.job.callback.duplicate jobId={} status={} attempt={} requestId={}",
                job.getJobId(), job.getStatus(), attempt, workerRequestId);
    }

    private Double calculateQueuedAgeSeconds(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null || finishedAt == null) {
            return null;
        }
        return Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null) {
            return;
        }
        double queuedAgeSeconds = Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
        meterRegistry.summary("scrape.job.queued.age.seconds").record(queuedAgeSeconds);
    }

    private String extractJobId(String rawBody) {
        try {
            return objectMapper.readTree(rawBody).path("job_id").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }

    private String hashRawBody(String rawBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return "";
        }
    }

    private int parseAttempt(String attemptHeader) {
        if (attemptHeader == null || attemptHeader.isBlank()) {
            return 1;
        }
        try {
            int value = Integer.parseInt(attemptHeader);
            return value <= 0 ? 1 : value;
        } catch (NumberFormatException e) {
            log.warn("[BIZ] scrape.job.callback.attempt.parse_fail header={} message={}", attemptHeader, e.getMessage());
            return 1;
        }
    }

    private String normalizeWorkerRequestId(String workerRequestId) {
        return workerRequestId == null ? "" : workerRequestId;
    }

    private JsonNode normalizeNodeKeys(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (JsonNode element : node) {
                arrayNode.add(normalizeNodeKeys(element));
            }
            return arrayNode;
        }
        if (!node.isObject()) {
            return node;
        }

        ObjectNode normalized = objectMapper.createObjectNode();
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            normalized.set(toCamelCase(fieldName), normalizeNodeKeys(node.get(fieldName)));
        }
        return normalized;
    }

    private String toCamelCase(String value) {
        if (value == null || value.isBlank() || !value.contains("_")) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean upperNext = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return builder.toString();
    }

    private String topLevelFieldNames(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(fieldNames.next());
        }
        return builder.toString();
    }
}
