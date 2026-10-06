package com.chukchuk.haksa.domain.scrapejob.model;

import com.chukchuk.haksa.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 포털 스크래핑 요청의 멱등성, 시도 횟수, 결과 위치 및 처리 상태를 관리한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "scrape_jobs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_scrape_jobs_user_idempotency",
          columnNames = {"user_id", "idempotency_key"})
    })
public class ScrapeJob extends BaseEntity {

  @Id
  @Column(name = "job_id", nullable = false, updatable = false)
  private String jobId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "portal_type", nullable = false, updatable = false)
  private String portalType;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, updatable = false)
  private ScrapeJobOperationType operationType;

  @Column(name = "idempotency_key", nullable = false, updatable = false)
  private String idempotencyKey;

  @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 64)
  private String requestFingerprint;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ScrapeJobStatus status;

  @Lob
  @Column(name = "request_payload_json", nullable = false, columnDefinition = "TEXT")
  private String requestPayloadJson;

  @Lob
  @Column(name = "result_payload_json", columnDefinition = "TEXT")
  private String resultPayloadJson;

  @Column(name = "result_s3_key")
  private String resultS3Key;

  @Column(name = "result_checksum")
  private String resultChecksum;

  @Column(name = "callback_attempt")
  private Integer callbackAttempt;

  @Column(name = "callback_received_at")
  private Instant callbackReceivedAt;

  @Lob
  @Column(name = "callback_metadata_json", columnDefinition = "TEXT")
  private String callbackMetadataJson;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "retryable")
  private Boolean retryable;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "link_started_at", nullable = false, updatable = false)
  private Instant linkStartedAt;

  @Column(name = "link_ended_at")
  private Instant linkEndedAt;

  private ScrapeJob(
      String jobId,
      UUID userId,
      String portalType,
      ScrapeJobOperationType operationType,
      String idempotencyKey,
      String requestFingerprint,
      ScrapeJobStatus status,
      String requestPayloadJson,
      Instant linkStartedAt) {
    this.jobId = jobId;
    this.userId = userId;
    this.portalType = portalType;
    this.operationType = operationType;
    this.idempotencyKey = idempotencyKey;
    this.requestFingerprint = requestFingerprint;
    this.status = status;
    this.requestPayloadJson = requestPayloadJson;
    this.linkStartedAt = linkStartedAt;
  }

  /**
   * 요청 시각을 연동 시작 시각으로 사용하는 대기 작업을 생성한다.
   *
   * @param userId 사용자 식별자
   * @param portalType 포털 유형
   * @param operationType 작업 유형
   * @param idempotencyKey 멱등성 키
   * @param requestFingerprint 요청 fingerprint 정보
   * @param requestPayloadJson 요청 payload json 정보
   * @return 접수 시각을 시작 시각으로 사용하는 대기 작업
   */
  public static ScrapeJob createQueued(
      UUID userId,
      String portalType,
      ScrapeJobOperationType operationType,
      String idempotencyKey,
      String requestFingerprint,
      String requestPayloadJson) {
    return createQueued(
        userId,
        portalType,
        operationType,
        idempotencyKey,
        requestFingerprint,
        requestPayloadJson,
        Instant.now());
  }

  /**
   * 별도로 측정한 연동 시작 시각을 사용하는 대기 작업을 생성한다.
   *
   * @param userId 사용자 식별자
   * @param portalType 포털 유형
   * @param operationType 작업 유형
   * @param idempotencyKey 멱등성 키
   * @param requestFingerprint 요청 fingerprint 정보
   * @param requestPayloadJson 요청 payload json 정보
   * @param linkStartedAt 포털 연동을 시작한 시각
   * @return 지정한 연동 시작 시각을 사용하는 대기 작업
   */
  public static ScrapeJob createQueued(
      UUID userId,
      String portalType,
      ScrapeJobOperationType operationType,
      String idempotencyKey,
      String requestFingerprint,
      String requestPayloadJson,
      Instant linkStartedAt) {
    return new ScrapeJob(
        UUID.randomUUID().toString(),
        userId,
        portalType,
        operationType,
        idempotencyKey,
        requestFingerprint,
        ScrapeJobStatus.QUEUED,
        requestPayloadJson,
        linkStartedAt);
  }

  /**
   * 요청 본문에서 계산한 지문이 저장된 작업 지문과 같은지 확인한다.
   *
   * @param requestFingerprint 비교할 요청 지문
   * @return 저장된 요청 지문과 같으면 {@code true}
   */
  public boolean hasSameFingerprint(String requestFingerprint) {
    return this.requestFingerprint.equals(requestFingerprint);
  }

  /**
   * 작업이 성공 또는 실패의 최종 상태에 도달했는지 확인한다.
   *
   * @return 작업 상태가 {@code SUCCEEDED} 또는 {@code FAILED}이면 {@code true}
   */
  public boolean isCompleted() {
    return status == ScrapeJobStatus.SUCCEEDED || status == ScrapeJobStatus.FAILED;
  }

  /**
   * 워커가 저장한 결과 payload가 있는지 확인한다.
   *
   * @return 결과 payload가 저장돼 있으면 {@code true}
   */
  public boolean hasWorkerResult() {
    return resultPayloadJson != null;
  }

  /**
   * 주어진 콜백 시도가 이미 처리한 시도보다 이전이거나 같은지 확인한다.
   *
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @return 중복되거나 오래된 콜백 시도이면 {@code true}
   */
  public boolean hasProcessedAttempt(int attempt) {
    return callbackAttempt != null && attempt <= callbackAttempt;
  }

  /** 스크래핑 작업을 실행 중 상태로 전환한다. */
  public void markRunning() {
    if (!isCompleted()) {
      this.status = ScrapeJobStatus.RUNNING;
    }
  }

  /**
   * 스크래핑 작업을 후처리 상태로 전환한다.
   *
   * @param resultS3Key 워커 결과 객체를 읽을 S3 키
   * @param resultChecksum 워커 결과 객체의 무결성을 확인할 체크섬
   * @param callbackMetadataJson 콜백 메타데이터 JSON
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @param receivedAt 수신 시각
   */
  public void markPostProcessing(
      String resultS3Key,
      String resultChecksum,
      String callbackMetadataJson,
      int attempt,
      Instant receivedAt) {
    this.status = ScrapeJobStatus.POST_PROCESSING;
    this.resultS3Key = resultS3Key;
    this.resultChecksum = resultChecksum;
    this.callbackMetadataJson = callbackMetadataJson;
    this.errorCode = null;
    this.errorMessage = null;
    this.retryable = null;
    recordCallbackAttempt(attempt, receivedAt);
  }

  /**
   * 스크래핑 작업의 성공 결과와 종료 시각을 기록한다.
   *
   * @param resultPayloadJson 결과 JSON payload
   * @param finishedAt 처리 종료 시각
   */
  public void markSucceeded(String resultPayloadJson, Instant finishedAt) {
    markSucceeded(resultPayloadJson, finishedAt, Instant.now());
  }

  /**
   * 스크래핑 작업의 성공 결과와 종료 시각을 기록한다.
   *
   * @param resultPayloadJson 결과 JSON payload
   * @param finishedAt 처리 종료 시각
   * @param linkEndedAt 포털 연결 단계가 종료된 시각
   */
  public void markSucceeded(String resultPayloadJson, Instant finishedAt, Instant linkEndedAt) {
    recordWorkerResult(resultPayloadJson, finishedAt);
    this.status = ScrapeJobStatus.SUCCEEDED;
    this.linkEndedAt = linkEndedAt;
  }

  /**
   * 워커가 생성한 결과와 종료 시각을 기록한다.
   *
   * @param resultPayloadJson 결과 JSON payload
   * @param finishedAt 처리 종료 시각
   */
  public void recordWorkerResult(String resultPayloadJson, Instant finishedAt) {
    this.resultPayloadJson = resultPayloadJson;
    this.errorCode = null;
    this.errorMessage = null;
    this.retryable = null;
    this.finishedAt = finishedAt;
  }

  /**
   * 콜백 수신 시도 횟수와 시각을 기록한다.
   *
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @param receivedAt 수신 시각
   */
  public void recordCallbackAttempt(int attempt, Instant receivedAt) {
    this.callbackAttempt = attempt;
    this.callbackReceivedAt = receivedAt;
  }

  /**
   * 스크래핑 결과 저장 위치와 콜백 정보를 기록한다.
   *
   * @param resultS3Key 워커 결과 객체를 읽을 S3 키
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @param receivedAt 수신 시각
   */
  public void recordResultLocation(String resultS3Key, int attempt, Instant receivedAt) {
    recordCallbackAttempt(attempt, receivedAt);
    this.resultS3Key = resultS3Key;
  }

  /**
   * 실패 콜백의 원인과 처리 정보를 기록한다.
   *
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @param receivedAt 수신 시각
   * @param callbackMetadataJson 콜백 메타데이터 JSON
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param finishedAt 처리 종료 시각
   */
  public void recordFailedCallback(
      int attempt,
      Instant receivedAt,
      String callbackMetadataJson,
      String errorCode,
      String errorMessage,
      Boolean retryable,
      Instant finishedAt) {
    recordFailedCallback(
        attempt,
        receivedAt,
        callbackMetadataJson,
        errorCode,
        errorMessage,
        retryable,
        finishedAt,
        Instant.now());
  }

  /**
   * 실패 콜백의 원인과 처리 정보를 기록한다.
   *
   * @param attempt 콜백 중복과 순서를 판정할 워커 시도 번호
   * @param receivedAt 수신 시각
   * @param callbackMetadataJson 콜백 메타데이터 JSON
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param finishedAt 처리 종료 시각
   * @param linkEndedAt 포털 연결 단계가 종료된 시각
   */
  public void recordFailedCallback(
      int attempt,
      Instant receivedAt,
      String callbackMetadataJson,
      String errorCode,
      String errorMessage,
      Boolean retryable,
      Instant finishedAt,
      Instant linkEndedAt) {
    recordCallbackAttempt(attempt, receivedAt);
    this.callbackMetadataJson = callbackMetadataJson;
    markFailed(errorCode, errorMessage, retryable, finishedAt, linkEndedAt);
  }

  /**
   * 처리 실패 상태와 원인을 기록한다.
   *
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param finishedAt 처리 종료 시각
   */
  public void markFailed(
      String errorCode, String errorMessage, Boolean retryable, Instant finishedAt) {
    markFailed(errorCode, errorMessage, retryable, finishedAt, Instant.now());
  }

  /**
   * 처리 실패 상태와 원인을 기록한다.
   *
   * @param errorCode 오류 코드
   * @param errorMessage 오류 응답 메시지
   * @param retryable 실패 후 재시도 가능한지 여부
   * @param finishedAt 처리 종료 시각
   * @param linkEndedAt 포털 연결 단계가 종료된 시각
   */
  public void markFailed(
      String errorCode,
      String errorMessage,
      Boolean retryable,
      Instant finishedAt,
      Instant linkEndedAt) {
    this.status = ScrapeJobStatus.FAILED;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.retryable = retryable;
    this.finishedAt = finishedAt;
    this.linkEndedAt = linkEndedAt;
  }
}
