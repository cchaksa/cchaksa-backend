package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.logging.sentry.SentryMdcContext;
import com.chukchuk.haksa.infrastructure.portal.dto.raw.RawPortalData;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.mapper.PortalDataMapper;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** 포털 스크래핑 콜백 이후 학사 데이터 반영을 처리한다. */
@Slf4j
@Component
public class PortalCallbackPostProcessor {

  private static final String FAILED_POST_PROCESSING = "FAILED_POST_PROCESSING";

  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final ScrapeResultCallbackTxService scrapeResultCallbackTxService;

  /**
   * 포털 결과 payload 역직렬화, 처리 지표 및 트랜잭션 반영 의존성으로 후처리기를 생성한다.
   *
   * @param objectMapper 포털 결과 payload를 역직렬화할 mapper
   * @param meterRegistry 후처리 성공·실패와 소요 시간을 기록할 지표 저장소
   * @param scrapeResultCallbackTxService 동기화 결과를 작업 상태에 반영할 트랜잭션 서비스
   */
  public PortalCallbackPostProcessor(
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry,
      ScrapeResultCallbackTxService scrapeResultCallbackTxService) {
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.scrapeResultCallbackTxService = scrapeResultCallbackTxService;
  }

  /**
   * 성공 콜백 payload를 포털 데이터로 변환해 사용자 학사 정보에 반영한다.
   *
   * <p>후처리 실패는 예외를 전파하지 않고 작업 실패 상태와 지표로 기록한다.
   *
   * @param jobId 작업 식별자
   * @param userId 사용자 식별자
   * @param operationType 작업 유형
   * @param payloadJson JSON payload
   * @param finishedAt 처리 종료 시각
   * @param queuedAgeSeconds 작업 생성부터 완료까지의 대기 시간(초)
   * @param attempt 콜백을 발생시킨 작업 시도 횟수
   * @param workerRequestId 워커 요청 식별자
   * @param payloadHash 로그 추적에 사용할 원본 payload 해시
   */
  public void process(
      String jobId,
      UUID userId,
      ScrapeJobOperationType operationType,
      String payloadJson,
      Instant finishedAt,
      Double queuedAgeSeconds,
      int attempt,
      String workerRequestId,
      String payloadHash) {
    try (SentryMdcContext.MdcScope ignored =
        SentryMdcContext.open(
            SentryMdcContext.from(userId, jobId, null, operationType, workerRequestId))) {
      processWithContext(
          jobId,
          userId,
          operationType,
          payloadJson,
          finishedAt,
          queuedAgeSeconds,
          attempt,
          workerRequestId,
          payloadHash);
    }
  }

  private void processWithContext(
      String jobId,
      UUID userId,
      ScrapeJobOperationType operationType,
      String payloadJson,
      Instant finishedAt,
      Double queuedAgeSeconds,
      int attempt,
      String workerRequestId,
      String payloadHash) {
    long startedAt = System.nanoTime();
    PortalData portalData;
    try {
      portalData = toPortalData(payloadJson);
    } catch (JsonProcessingException e) {
      handleParsingFailure(jobId, userId, operationType, e.getOriginalMessage(), e);
      return;
    } catch (RuntimeException e) {
      handleParsingFailure(jobId, userId, operationType, e.getMessage(), e);
      return;
    }

    String studentCode = portalData.student().studentCode();
    log.info(
        "[BIZ] scrape.job.callback.postprocess.start jobId={} userId={} "
            + "operationType={} studentCode={} attempt={} requestId={} payloadHash={}",
        jobId,
        userId,
        operationType,
        studentCode,
        attempt,
        workerRequestId,
        payloadHash);
    try {
      scrapeResultCallbackTxService.completeSuccess(
          jobId,
          userId,
          operationType,
          portalData,
          payloadJson,
          finishedAt,
          queuedAgeSeconds,
          payloadHash);
      meterRegistry.counter("scrape.job.callback.postprocess.success").increment();
      long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
      meterRegistry
          .timer("scrape.job.callback.stage", "stage", "postprocess_tx")
          .record(Duration.ofMillis(elapsedMs));
      log.info(
          "[BIZ] scrape.job.callback.postprocess.success jobId={} userId={} "
              + "operationType={} studentCode={} elapsed_ms={}",
          jobId,
          userId,
          operationType,
          studentCode,
          elapsedMs);
    } catch (EntityNotFoundException exception) {
      recordFailure(jobId, finishedAt, queuedAgeSeconds, "user_missing", operationType, exception);
    } catch (PortalScrapeException exception) {
      recordFailure(
          jobId, finishedAt, queuedAgeSeconds, "portal_conn_fail", operationType, exception);
    } catch (DataIntegrityViolationException exception) {
      recordFailure(
          jobId, finishedAt, queuedAgeSeconds, "data_integrity", operationType, exception);
    } catch (RuntimeException exception) {
      recordFailure(jobId, finishedAt, queuedAgeSeconds, "unexpected", operationType, exception);
    }
  }

  private void handleParsingFailure(
      String jobId,
      UUID userId,
      ScrapeJobOperationType operationType,
      String message,
      Exception exception) {
    meterRegistry
        .counter("scrape.job.callback.postprocess.fail", "reason", "invalid_payload")
        .increment();
    log.warn(
        "[BIZ] scrape.job.callback.postprocess.fail jobId={} userId={} "
            + "operationType={} reason=invalid_payload message={}",
        jobId,
        userId,
        operationType,
        message,
        exception);
    throw new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID, exception);
  }

  private void recordFailure(
      String jobId,
      Instant finishedAt,
      Double queuedAgeSeconds,
      String reason,
      ScrapeJobOperationType operationType,
      Exception exception) {
    meterRegistry.counter("scrape.job.callback.postprocess.fail", "reason", reason).increment();
    String failureDetail = failureCode(reason, exception, operationType);
    scrapeResultCallbackTxService.markFailed(
        jobId,
        finishedAt,
        queuedAgeSeconds,
        FAILED_POST_PROCESSING,
        failureDetail + ":" + exception.getMessage(),
        false);
    log.warn(
        "[BIZ] scrape.job.callback.postprocess.fail jobId={} operationType={} reason={} detail={}",
        jobId,
        operationType,
        reason,
        failureDetail,
        exception);
    throw new CommonException(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED, exception);
  }

  private PortalData toPortalData(String payloadJson) throws JsonProcessingException {
    RawPortalData rawPortalData = objectMapper.readValue(payloadJson, RawPortalData.class);
    return PortalDataMapper.toPortalData(rawPortalData);
  }

  private String failureCode(
      String reason, Exception exception, ScrapeJobOperationType operationType) {
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
