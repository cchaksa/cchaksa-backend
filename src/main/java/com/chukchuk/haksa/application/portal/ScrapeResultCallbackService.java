package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.infrastructure.portal.client.ScrapeResultResultStoreClient;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeResultCallbackService {

    private static final String FAILED_S3_READ = "FAILED_S3_READ";
    private static final String FAILED_RESULT_SCHEMA = "FAILED_RESULT_SCHEMA";

    private final ScrapeJobRepository scrapeJobRepository;
    private final PortalCallbackPostProcessor portalCallbackPostProcessor;
    private final ScrapeResultResultStoreClient resultStoreClient;
    private final HmacSignatureVerifier hmacSignatureVerifier;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Transactional
    public void handleCallback(String rawBody, String timestamp, String signature, String attemptHeader, String workerRequestId) {
        String bodyHash = hashRawBody(rawBody);
        String hintedJobId = extractJobId(rawBody);
        PortalLinkDto.ScrapeResultCallbackRequest request = parseRequest(rawBody, bodyHash);
        int attempt = resolveAttempt(attemptHeader, request.attempt());
        String normalizedWorkerRequestId = normalizeWorkerRequestId(workerRequestId);

        try {
            hmacSignatureVerifier.verify(timestamp, rawBody, signature);
        } catch (CommonException exception) {
            HmacSignatureVerifier.VerificationDiagnostics diagnostics =
                    hmacSignatureVerifier.diagnostics(timestamp, rawBody, signature);
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

        ScrapeJob job = scrapeJobRepository.findForUpdateByJobId(request.job_id())
                .orElseThrow(() -> {
                    log.warn("[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
                            request.job_id(), bodyHash);
                    return new EntityNotFoundException(ErrorCode.SCRAPE_JOB_NOT_FOUND);
                });

        log.info("[BIZ] scrape.job.callback.received jobId={} status={} attempt={} requestId={} resultS3Key={} rawBodyHash={}",
                job.getJobId(), request.status(), attempt, normalizedWorkerRequestId, request.result_s3_key(), bodyHash);

        if (job.hasProcessedAttempt(attempt) || job.isCompleted()) {
            handleDuplicate(job, attempt, normalizedWorkerRequestId, request.result_s3_key());
            return;
        }
        job.recordCallbackAttempt(attempt, Instant.now());

        Instant finishedAt = request.finished_at() == null ? Instant.now() : request.finished_at();
        String normalizedStatus = normalize(request.status());

        if ("succeeded".equals(normalizedStatus)) {
            handleSucceeded(job, request, finishedAt, attempt, normalizedWorkerRequestId);
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
            int attempt,
            String workerRequestId
    ) {
        validateResultKey(job.getJobId(), request.result_s3_key());
        job.recordResultLocation(request.result_s3_key(), attempt, Instant.now());

        try {
            String normalizedPayloadJson = fetchAndNormalizePayload(request.result_s3_key());

            job.recordWorkerResult(normalizedPayloadJson, finishedAt);
            Double queuedAgeSeconds = calculateQueuedAgeSeconds(job, finishedAt);
            recordQueuedAge(job, finishedAt);
            meterRegistry.counter("scrape.job.callback.persisted").increment();
            String payloadHash = hashRawBody(normalizedPayloadJson);
            log.info("[BIZ] scrape.job.callback.persisted jobId={} attempt={} requestId={} payloadHash={}",
                    job.getJobId(), attempt, workerRequestId, payloadHash);

            portalCallbackPostProcessor.process(
                    job.getJobId(),
                    job.getUserId(),
                    job.getOperationType(),
                    normalizedPayloadJson,
                    finishedAt,
                    queuedAgeSeconds,
                    attempt,
                    workerRequestId,
                    payloadHash
            );
        } catch (ScrapeResultPayloadAccessException exception) {
            job.markFailed(FAILED_S3_READ, exception.getMessage(), exception.isRetryable(), finishedAt);
            recordQueuedAge(job, finishedAt);
            log.error("[BIZ] scrape.job.s3.fail jobId={} key={} attempt={} reason={}",
                    job.getJobId(), request.result_s3_key(), attempt, exception.getMessage());
            throw new CommonException(ErrorCode.SCRAPE_RESULT_S3_FAILED, exception);
        } catch (JsonProcessingException exception) {
            job.markFailed(FAILED_RESULT_SCHEMA, exception.getOriginalMessage(), false, finishedAt);
            recordQueuedAge(job, finishedAt);
            log.warn("[BIZ] scrape.job.callback.invalid_payload stage=result_payload_parse jobId={} resultS3Key={} message={}",
                    job.getJobId(), request.result_s3_key(), exception.getOriginalMessage());
            throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID, exception);
        }
    }

    private String fetchAndNormalizePayload(String resultS3Key) throws JsonProcessingException {
        String payload = resultStoreClient.fetch(resultS3Key);
        JsonNode original = objectMapper.readTree(payload);
        JsonNode normalized = normalizeNodeKeys(original);
        return writeJson(normalized);
    }

    private void handleDuplicate(ScrapeJob job, int attempt, String workerRequestId, String resultS3Key) {
        meterRegistry.counter("scrape.job.callback.duplicate", "status", job.getStatus().name().toLowerCase(Locale.ROOT))
                .increment();
        log.info("[BIZ] scrape.job.callback.duplicate jobId={} status={} attempt={} requestId={} resultS3Key={}",
                job.getJobId(), job.getStatus(), attempt, workerRequestId, resultS3Key);
    }

    private Double calculateQueuedAgeSeconds(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null || finishedAt == null) {
            return null;
        }
        return Duration.between(job.getCreatedAt(), finishedAt).toMillis() / 1000.0;
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
    }

    private void recordQueuedAge(ScrapeJob job, Instant finishedAt) {
        if (job.getCreatedAt() == null || finishedAt == null) {
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

    private int resolveAttempt(String attemptHeader, Integer attemptFromPayload) {
        if (attemptFromPayload != null && attemptFromPayload > 0) {
            return attemptFromPayload;
        }
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

    private void validateResultKey(String jobId, String resultS3Key) {
        if (resultS3Key == null || resultS3Key.isBlank()) {
            throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY);
        }
        if (!resultS3Key.contains(jobId)) {
            log.warn("[BIZ] scrape.job.callback.s3_key_without_job jobId={} resultS3Key={}", jobId, resultS3Key);
            throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY);
        }
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }
}
