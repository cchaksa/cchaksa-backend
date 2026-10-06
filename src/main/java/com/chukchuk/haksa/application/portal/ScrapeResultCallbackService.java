package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.logging.sentry.SentryMdcContext;
import com.chukchuk.haksa.infrastructure.portal.client.ScrapeResultStoreClient;
import com.chukchuk.haksa.infrastructure.portal.exception.ScrapeResultPayloadAccessException;
import com.chukchuk.haksa.infrastructure.security.HmacSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 워커 콜백의 서명과 payload를 검증하고 스크래핑 작업 상태 및 학사 데이터를 반영한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeResultCallbackService {

  private static final String FAILED_S3_READ = "FAILED_S3_READ";
  private static final String FAILED_RESULT_SCHEMA = "FAILED_RESULT_SCHEMA";
  private static final Pattern JOB_ID_PATTERN = Pattern.compile("\"job_id\"\\s*:\\s*\"([^\"]+)\"");

  private final PortalCallbackPostProcessor portalCallbackPostProcessor;
  private final ScrapeResultCallbackTxService scrapeResultCallbackTxService;
  private final ScrapeResultStoreClient resultStoreClient;
  private final HmacSignatureVerifier hmacSignatureVerifier;
  private final MeterRegistry meterRegistry;
  private final ObjectMapper objectMapper;

  /**
   * 워커 콜백의 서명·상태·결과 위치를 검증하고 성공 또는 실패 처리를 실행한다.
   *
   * @param rawBody 서명 검증 대상 요청 본문
   * @param timestamp 요청 타임스탬프
   * @param signature 요청 서명
   * @param attemptHeader payload에 시도 횟수가 없을 때 사용할 요청 헤더
   * @param workerRequestId 로그와 오류 추적에 사용할 워커 요청 식별자
   * @throws CommonException 서명, payload 형식, 결과 위치 또는 checksum이 유효하지 않은 경우
   * @throws EntityNotFoundException 콜백 대상 작업을 찾을 수 없는 경우
   */
  public void handleCallback(
      String rawBody,
      String timestamp,
      String signature,
      String attemptHeader,
      String workerRequestId) {
    long startedAt = System.nanoTime();
    String bodyHash = hashRawBody(rawBody);
    String hintedJobId = extractJobId(rawBody);

    try {
      hmacSignatureVerifier.verify(timestamp, rawBody, signature);
    } catch (CommonException exception) {
      HmacSignatureVerifier.VerificationDiagnostics diagnostics =
          hmacSignatureVerifier.diagnostics(timestamp, rawBody, signature);
      log.warn(
          "[BIZ] scrape.job.callback.invalid_signature jobId={} "
              + "signatureValid=false reason={} timestamp={} parsedTimestamp={} "
              + "timestampDeltaSeconds={} rawBodyHash={} actualSignatureEncoding={} "
              + "actualSignatureLength={} actualSignatureHash={} "
              + "expectedUtf8SignatureHash={} expectedHexSignatureHash={}",
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
    int attempt = resolveAttempt(attemptHeader, request.attempt());
    String normalizedWorkerRequestId = normalizeWorkerRequestId(workerRequestId);
    String callbackMetadataJson = writeJson(request.metadata());
    String normalizedStatus = normalize(request.status());
    Instant callbackReceivedAt = Instant.now();
    Instant finishedAt = request.finishedAt() == null ? Instant.now() : request.finishedAt();
    logStage(
        "validated",
        request.jobId(),
        attempt,
        normalizedStatus,
        request.resultS3Key(),
        normalizedWorkerRequestId,
        elapsedMillis(startedAt));

    if ("succeeded".equals(normalizedStatus)) {
      handleSucceeded(
          request,
          callbackMetadataJson,
          callbackReceivedAt,
          finishedAt,
          attempt,
          normalizedWorkerRequestId,
          bodyHash,
          startedAt);
      return;
    }

    if ("failed".equals(normalizedStatus)) {
      handleFailed(
          request,
          callbackMetadataJson,
          callbackReceivedAt,
          finishedAt,
          attempt,
          normalizedWorkerRequestId,
          bodyHash,
          startedAt);
      return;
    }

    throw new CommonException(ErrorCode.SCRAPE_INVALID_CALLBACK_REQUEST);
  }

  private PortalLinkDto.ScrapeResultCallbackRequest parseRequest(String rawBody, String bodyHash) {
    try {
      return objectMapper.readValue(rawBody, PortalLinkDto.ScrapeResultCallbackRequest.class);
    } catch (JsonProcessingException e) {
      log.warn(
          "[BIZ] scrape.job.callback.invalid_payload stage=request_parse rawBodyHash={} message={}",
          bodyHash,
          e.getOriginalMessage());
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
      long startedAt) {
    validateResultKey(request.jobId(), request.resultS3Key());
    long receiptStartedAt = System.nanoTime();
    ScrapeResultCallbackTxService.CallbackReceipt receipt =
        receiveSuccessCallback(
            request, callbackMetadataJson, callbackReceivedAt, attempt, bodyHash);
    SentryMdcContext.Context context = contextFor(receipt, workerRequestId);
    SentryMdcContext.bindToCurrentRequest(context);
    try (SentryMdcContext.MdcScope ignored = SentryMdcContext.open(context)) {
      logStage(
          "receipt_committed",
          receipt.jobId(),
          attempt,
          receipt.status(),
          request.resultS3Key(),
          workerRequestId,
          elapsedMillis(receiptStartedAt));
      if (receipt.duplicate()) {
        handleDuplicate(receipt, attempt, workerRequestId, request.resultS3Key());
        return;
      }

      try {
        long s3StartedAt = System.nanoTime();
        PayloadBundle payloadBundle = fetchAndNormalizePayload(request.resultS3Key());
        verifyChecksum(request.resultChecksum(), payloadBundle.rawPayloadJson());
        logStage(
            "payload_ready",
            receipt.jobId(),
            attempt,
            receipt.status(),
            request.resultS3Key(),
            workerRequestId,
            elapsedMillis(s3StartedAt));

        meterRegistry.counter("scrape.job.callback.persisted").increment();
        String payloadHash = hashRawBody(payloadBundle.rawPayloadJson());
        log.info(
            "[BIZ] scrape.job.callback.persisted jobId={} attempt={} requestId={} payloadHash={}",
            receipt.jobId(),
            attempt,
            workerRequestId,
            payloadHash);

        long postProcessStartedAt = System.nanoTime();
        portalCallbackPostProcessor.process(
            receipt.jobId(),
            receipt.userId(),
            receipt.operationType(),
            payloadBundle.normalizedPayloadJson(),
            finishedAt,
            receipt.queuedAgeSeconds(),
            attempt,
            workerRequestId,
            payloadHash);
        logStage(
            "postprocess_committed",
            receipt.jobId(),
            attempt,
            "succeeded",
            request.resultS3Key(),
            workerRequestId,
            elapsedMillis(postProcessStartedAt));
      } catch (CommonException exception) {
        if (shouldMarkSchemaFailure(exception)) {
          scrapeResultCallbackTxService.markFailed(
              receipt.jobId(),
              finishedAt,
              receipt.queuedAgeSeconds(),
              FAILED_RESULT_SCHEMA,
              exception.getMessage(),
              false);
        }
        throw exception;
      } catch (ScrapeResultPayloadAccessException exception) {
        scrapeResultCallbackTxService.markFailed(
            receipt.jobId(),
            finishedAt,
            receipt.queuedAgeSeconds(),
            FAILED_S3_READ,
            exception.getMessage(),
            exception.isRetryable());
        log.warn(
            "[BIZ] scrape.job.s3.fail jobId={} key={} attempt={} reason={}",
            receipt.jobId(),
            request.resultS3Key(),
            attempt,
            exception.getMessage());
        throw new CommonException(ErrorCode.SCRAPE_RESULT_S3_FAILED, exception);
      } catch (JsonProcessingException exception) {
        scrapeResultCallbackTxService.markFailed(
            receipt.jobId(),
            finishedAt,
            receipt.queuedAgeSeconds(),
            FAILED_RESULT_SCHEMA,
            exception.getOriginalMessage(),
            false);
        log.warn(
            "[BIZ] scrape.job.callback.invalid_payload stage=result_payload_parse "
                + "jobId={} resultS3Key={} message={}",
            receipt.jobId(),
            request.resultS3Key(),
            exception.getOriginalMessage());
        throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID, exception);
      } finally {
        logStage(
            "completed",
            receipt.jobId(),
            attempt,
            normalizedStatus(request.status()),
            request.resultS3Key(),
            workerRequestId,
            elapsedMillis(startedAt));
      }
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
      long startedAt) {
    ScrapeResultCallbackTxService.CallbackReceipt receipt =
        receiveFailedCallback(
            request, callbackMetadataJson, callbackReceivedAt, finishedAt, attempt, bodyHash);
    SentryMdcContext.Context context = contextFor(receipt, workerRequestId);
    SentryMdcContext.bindToCurrentRequest(context);
    try (SentryMdcContext.MdcScope ignored = SentryMdcContext.open(context)) {
      if (receipt.duplicate()) {
        handleDuplicate(receipt, attempt, workerRequestId, request.resultS3Key());
        return;
      }
      log.info(
          "[BIZ] scrape.job.failed jobId={} errorCode={} retryable={} attempt={} requestId={}",
          receipt.jobId(),
          request.errorCode(),
          request.retryable(),
          attempt,
          workerRequestId);
      logStage(
          "completed",
          receipt.jobId(),
          attempt,
          receipt.status(),
          request.resultS3Key(),
          workerRequestId,
          elapsedMillis(startedAt));
    }
  }

  private SentryMdcContext.Context contextFor(
      ScrapeResultCallbackTxService.CallbackReceipt receipt, String workerRequestId) {
    return SentryMdcContext.from(
        receipt.userId(), receipt.jobId(), null, receipt.operationType(), workerRequestId);
  }

  private PayloadBundle fetchAndNormalizePayload(String resultS3Key)
      throws JsonProcessingException {
    String rawPayload = resultStoreClient.fetch(resultS3Key);
    JsonNode original = objectMapper.readTree(rawPayload);
    JsonNode normalized = normalizeNodeKeys(original);
    return new PayloadBundle(rawPayload, writeJson(normalized));
  }

  private void handleDuplicate(
      ScrapeResultCallbackTxService.CallbackReceipt receipt,
      int attempt,
      String workerRequestId,
      String resultS3Key) {
    meterRegistry.counter("scrape.job.callback.duplicate", "status", receipt.status()).increment();
    log.info(
        "[BIZ] scrape.job.callback.duplicate jobId={} status={} attempt={} "
            + "requestId={} resultS3Key={}",
        receipt.jobId(),
        receipt.status(),
        attempt,
        workerRequestId,
        resultS3Key);
  }

  private String normalize(String status) {
    return status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
  }

  private String extractJobId(String rawBody) {
    Matcher matcher = JOB_ID_PATTERN.matcher(rawBody);
    return matcher.find() ? matcher.group(1) : "";
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
      log.warn(
          "[BIZ] scrape.job.callback.attempt.parse_fail header={} message={}",
          attemptHeader,
          e.getMessage());
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
    ScrapeResultStoreClient.S3Location location;
    try {
      location = resultStoreClient.validateLocation(resultS3Key);
    } catch (ScrapeResultPayloadAccessException exception) {
      log.warn(
          "[BIZ] scrape.job.callback.invalid_s3_key jobId={} resultS3Key={} reason={}",
          jobId,
          resultS3Key,
          exception.getMessage());
      throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY, exception);
    }
    if (!resultStoreClient.isJobScopedLocation(location, jobId)) {
      log.warn(
          "[BIZ] scrape.job.callback.s3_key_without_job jobId={} resultS3Key={}",
          jobId,
          resultS3Key);
      throw new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY);
    }
  }

  private void verifyChecksum(String expectedChecksum, String payloadJson) {
    if (expectedChecksum == null || expectedChecksum.isBlank()) {
      return;
    }

    String normalizedExpected = expectedChecksum.trim().toLowerCase(Locale.ROOT);
    String actualHash = hashRawBody(payloadJson);
    String expectedHash =
        normalizedExpected.startsWith("sha256:")
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
      String bodyHash) {
    try {
      return scrapeResultCallbackTxService.receiveSuccessCallback(
          request.jobId(),
          attempt,
          request.resultS3Key(),
          request.resultChecksum(),
          callbackMetadataJson,
          callbackReceivedAt);
    } catch (EntityNotFoundException exception) {
      log.warn(
          "[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
          request.jobId(),
          bodyHash);
      throw exception;
    }
  }

  private ScrapeResultCallbackTxService.CallbackReceipt receiveFailedCallback(
      PortalLinkDto.ScrapeResultCallbackRequest request,
      String callbackMetadataJson,
      Instant callbackReceivedAt,
      Instant finishedAt,
      int attempt,
      String bodyHash) {
    try {
      return scrapeResultCallbackTxService.receiveFailedCallback(
          request.jobId(),
          attempt,
          callbackMetadataJson,
          request.errorCode(),
          request.errorMessage(),
          request.retryable(),
          callbackReceivedAt,
          finishedAt);
    } catch (EntityNotFoundException exception) {
      log.warn(
          "[BIZ] scrape.job.callback.job_not_found jobId={} signatureValid=true rawBodyHash={}",
          request.jobId(),
          bodyHash);
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
      long elapsedMs) {
    meterRegistry
        .timer("scrape.job.callback.stage", "stage", stage)
        .record(Duration.ofMillis(elapsedMs));
    log.info(
        "[BIZ] scrape.job.callback.stage stage={} jobId={} attempt={} status={} "
            + "resultS3Key={} requestId={} elapsed_ms={}",
        stage,
        jobId,
        attempt,
        status,
        resultS3Key,
        workerRequestId,
        elapsedMs);
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

  private boolean shouldMarkSchemaFailure(CommonException exception) {
    return ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID.code().equals(exception.getCode())
        && ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED != exception.getErrorCode();
  }

  private record PayloadBundle(String rawPayloadJson, String normalizedPayloadJson) {}
}
