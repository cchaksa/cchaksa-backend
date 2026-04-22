package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
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

    private final PortalCallbackPostProcessor portalCallbackPostProcessor;
    private final ScrapeResultCallbackTxService scrapeResultCallbackTxService;
    private final ScrapeResultResultStoreClient resultStoreClient;
    private final HmacSignatureVerifier hmacSignatureVerifier;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void handleCallback(String rawBody, String timestamp, String signature, String attemptHeader, String workerRequestId) {
        long startedAt = System.nanoTime();
        String bodyHash = hashRawBody(rawBody);
        String hintedJobId = extractJobId(rawBody);
        PortalLinkDto.ScrapeResultCallbackRequest request = parseRequest(rawBody, bodyHash);
        int attempt = resolveAttempt(attemptHeader, request.attempt());
        String normalizedWorkerRequestId = normalizeWorkerRequestId(workerRequestId);
        String callbackMetadataJson = writeJson(request.metadata());
        String normalizedStatus = normalize(request.status());
        Instant callbackReceivedAt = Instant.now();

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

        Instant finishedAt = request.finished_at() == null ? Instant.now() : request.finished_at();
        logStage(
                "validated",
                request.job_id(),
                attempt,
                normalizedStatus,
                request.result_s3_key(),
                normalizedWorkerRequestId,
                elapsedMillis(startedAt)
        );

        if ("succeeded".equals(normalizedStatus)) {
            handleSucceeded(request, callbackMetadataJson, callbackReceivedAt, finishedAt, attempt, normalizedWorkerRequestId, bodyHash, startedAt);
            return;
        }

        if ("failed".equals(normalizedStatus)) {
            handleFailed(request, callbackMetadataJson, callbackReceivedAt, finishedAt, attempt, normalizedWorkerRequestId, bodyHash, startedAt);
            return;
        }

        throw new CommonException(ErrorCode.SCRAPE_INVALID_CALLBACK_REQUEST);
    }

    private PortalLinkDto.ScrapeResultCallbackRequest parseRequest(String rawBody, String bodyHash) {
        try {
            return objectMapper.readValue(rawBody, PortalLinkDto.ScrapeResultCallbackRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("[BIZ] scrape.job.callback.invalid_payload stage=request_parse rawBodyHash={} message={}",
                    bodyHash, e.getOriginalMessage());
            throw new CommonException(ErrorCode.SCRAPE_INVALID_CALLBACK_REQUEST, e);
        }
    }

    private void handleSucceeded(
            PortalLinkDto.ScrapeResultCallbackRequest request,
            String callbackMetadataJson,
            Instant callbackReceivedAt,
            Instant finishedAt,
            int attempt,
            String workerRequestId,
            String bodyHash,
            long startedAt
    ) {
        validateResultKey(request.job_id(), request.result_s3_key());
        long receiptStartedAt = System.nanoTime();
        ScrapeResultCallbackTxService.CallbackReceipt receipt = receiveSuccessCallback(
                request,
                callbackMetadataJson,
                callbackReceivedAt,
                attempt,
                bodyHash
        );
        logStage(
                "receipt_committed",
                receipt.jobId(),
                attempt,
                receipt.status(),
                request.result_s3_key(),
                workerRequestId,
                elapsedMillis(receiptStartedAt)
        );
        if (receipt.duplicate()) {
            handleDuplicate(receipt, attempt, workerRequestId, request.result_s3_key());
            return;
        }

        try {
            long s3StartedAt = System.nanoTime();
            String normalizedPayloadJson = fetchAndNormalizePayload(request.result_s3_key());
            verifyChecksum(request.resultChecksum(), normalizedPayloadJson);
            logStage(
                    "payload_ready",
                    receipt.jobId(),
                    attempt,
                    receipt.status(),
                    request.result_s3_key(),
                    workerRequestId,
                    elapsedMillis(s3StartedAt)
            );

            meterRegistry.counter("scrape.job.callback.persisted").increment();
            String payloadHash = hashRawBody(normalizedPayloadJson);
            log.info("[BIZ] scrape.job.callback.persisted jobId={} attempt={} requestId={} payloadHash={}",
                    receipt.jobId(), attempt, workerRequestId, payloadHash);

            long postProcessStartedAt = System.nanoTime();
            portalCallbackPostProcessor.process(
                    receipt.jobId(),
                    receipt.userId(),
                    receipt.operationType(),
                    normalizedPayloadJson,
                    finishedAt,
                    receipt.queuedAgeSeconds(),
                    attempt,
                    workerRequestId,
                    payloadHash
            );
            logStage(
                    "postprocess_committed",
                    receipt.jobId(),
                    attempt,
                    "succeeded",
                    request.result_s3_key(),
                    workerRequestId,
                    elapsedMillis(postProcessStartedAt)
            );
        } catch (CommonException exception) {
            if (ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID.code().equals(exception.getCode())) {
                scrapeResultCallbackTxService.markFailed(
                        receipt.jobId(),
                        finishedAt,
                        receipt.queuedAgeSeconds(),
                        FAILED_RESULT_SCHEMA,
                        exception.getMessage(),
                        false
                );
            }
            throw exception;
        } catch (ScrapeResultPayloadAccessException exception) {
            if (exception.isRetryable()) {
                log.error("[BIZ] scrape.job.s3.retryable jobId={} key={} attempt={} reason={}",
                        receipt.jobId(), request.result_s3_key(), attempt, exception.getMessage());
                throw new CommonException(ErrorCode.SCRAPE_RESULT_S3_FAILED, exception);
            }

            scrapeResultCallbackTxService.markFailed(
                    receipt.jobId(),
                    finishedAt,
                    receipt.queuedAgeSeconds(),
                    FAILED_S3_READ,
                    exception.getMessage(),
                    false
            );
            log.warn("[BIZ] scrape.job.s3.non_retryable jobId={} key={} attempt={} reason={}",
                    receipt.jobId(), request.result_s3_key(), attempt, exception.getMessage());
            throw new CommonException(ErrorCode.SCRAPE_RESULT_S3_ACCESS_DENIED, exception);
        } catch (JsonProcessingException exception) {
            scrapeResultCallbackTxService.markFailed(
                    receipt.jobId(),
                    finishedAt,
                    receipt.queuedAgeSeconds(),
                    FAILED_RESULT_SCHEMA,
                    exception.getOriginalMessage(),
                    false
            );
            log.warn("[BIZ] scrape.job.callback.invalid_payload stage=result_payload_parse jobId={} resultS3Key={} message={}",
                    receipt.jobId(), request.result_s3_key(), exception.getOriginalMessage());
            throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID, exception);
        } finally {
            logStage(
                    "completed",
                    receipt.jobId(),
                    attempt,
                    normalizedStatus(request.status()),
                    request.result_s3_key(),
                    workerRequestId,
                    elapsedMillis(startedAt)
            );
        }
    }

    private void handleFailed(
            PortalLinkDto.ScrapeResultCallbackRequest request,
            String callbackMetadataJson,
            Instant callbackReceivedAt,
            Instant finishedAt,
            int attempt,
            String workerRequestId,
            String bodyHash,
            long startedAt
    ) {
        ScrapeResultCallbackTxService.CallbackReceipt receipt = receiveFailedCallback(
                request,
                callbackMetadataJson,
                callbackReceivedAt,
                finishedAt,
                attempt,
                bodyHash
        );
        if (receipt.duplicate()) {
            handleDuplicate(receipt, attempt, workerRequestId, request.result_s3_key());
            return;
        }
        log.info("[BIZ] scrape.job.failed jobId={} errorCode={} retryable={} attempt={} requestId={}",
                receipt.jobId(), request.error_code(), request.retryable(), attempt, workerRequestId);
        logStage(
                "completed",
                receipt.jobId(),
                attempt,
                receipt.status(),
                request.result_s3_key(),
                workerRequestId,
                elapsedMillis(startedAt)
        );
    }

    private String fetchAndNormalizePayload(String resultS3Key) throws JsonProcessingException {
        String payload = resultStoreClient.fetch(resultS3Key);
        JsonNode original = objectMapper.readTree(payload);
        JsonNode normalized = normalizeNodeKeys(original);
        return writeJson(normalized);
    }

    private void handleDuplicate(
            ScrapeResultCallbackTxService.CallbackReceipt receipt,
            int attempt,
            String workerRequestId,
            String resultS3Key
    ) {
        meterRegistry.counter("scrape.job.callback.duplicate", "status", receipt.status())
                .increment();
        log.info("[BIZ] scrape.job.callback.duplicate jobId={} status={} attempt={} requestId={} resultS3Key={}",
                receipt.jobId(), receipt.status(), attempt, workerRequestId, resultS3Key);
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
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
        ScrapeResultResultStoreClient.S3Location location;
        try {
            location = resultStoreClient.validateLocation(resultS3Key);
        } catch (ScrapeResultPayloadAccessException exception) {
            log.warn("[BIZ] scrape.job.callback.invalid_s3_key jobId={} resultS3Key={} reason={}",
                    jobId, resultS3Key, exception.getMessage());
            throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY, exception);
        }
        if (!location.key().contains(jobId)) {
            log.warn("[BIZ] scrape.job.callback.s3_key_without_job jobId={} resultS3Key={}", jobId, resultS3Key);
            throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY);
        }
    }

    private void verifyChecksum(String expectedChecksum, String payloadJson) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            return;
        }

        String normalizedExpected = expectedChecksum.trim().toLowerCase(Locale.ROOT);
        String actualHash = hashRawBody(payloadJson);
        String expectedHash = normalizedExpected.startsWith("sha256:")
                ? normalizedExpected.substring("sha256:".length())
                : normalizedExpected;
        if (!actualHash.equals(expectedHash)) {
            throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID);
        }
    }

    private ScrapeResultCallbackTxService.CallbackReceipt receiveSuccessCallback(
            PortalLinkDto.ScrapeResultCallbackRequest request,
            String callbackMetadataJson,
            Instant callbackReceivedAt,
            int attempt,
            String bodyHash
    ) {
        try {
            return scrapeResultCallbackTxService.receiveSuccessCallback(
                    request.job_id(),
                    attempt,
                    request.result_s3_key(),
                    request.resultChecksum(),
                    callbackMetadataJson,
                    callbackReceivedAt
            );
        } catch (EntityNotFoundException exception) {
            log.warn("[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
                    request.job_id(), bodyHash);
            throw exception;
        }
    }

    private ScrapeResultCallbackTxService.CallbackReceipt receiveFailedCallback(
            PortalLinkDto.ScrapeResultCallbackRequest request,
            String callbackMetadataJson,
            Instant callbackReceivedAt,
            Instant finishedAt,
            int attempt,
            String bodyHash
    ) {
        try {
            return scrapeResultCallbackTxService.receiveFailedCallback(
                    request.job_id(),
                    attempt,
                    callbackMetadataJson,
                    request.error_code(),
                    request.error_message(),
                    request.retryable(),
                    callbackReceivedAt,
                    finishedAt
            );
        } catch (EntityNotFoundException exception) {
            log.warn("[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
                    request.job_id(), bodyHash);
            throw exception;
        }
    }

    private void logStage(
            String stage,
            String jobId,
            int attempt,
            String status,
            String resultS3Key,
            String workerRequestId,
            long elapsedMs
    ) {
        meterRegistry.timer("scrape.job.callback.stage", "stage", stage).record(Duration.ofMillis(elapsedMs));
        log.info("[BIZ] scrape.job.callback.stage stage={} jobId={} attempt={} status={} resultS3Key={} requestId={} elapsed_ms={}",
                stage, jobId, attempt, status, resultS3Key, workerRequestId, elapsedMs);
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String normalizedStatus(String status) {
        return normalize(status);
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
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT, e);
        }
    }
}
