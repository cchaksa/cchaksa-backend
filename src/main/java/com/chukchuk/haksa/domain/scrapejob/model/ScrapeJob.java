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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "scrape_jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scrape_jobs_user_idempotency", columnNames = {"user_id", "idempotency_key"})
        }
)
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

    @Column(name = "callback_attempt")
    private Integer callbackAttempt;

    @Column(name = "callback_received_at")
    private Instant callbackReceivedAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retryable")
    private Boolean retryable;

    @Column(name = "finished_at")
    private Instant finishedAt;

    private ScrapeJob(
            String jobId,
            UUID userId,
            String portalType,
            ScrapeJobOperationType operationType,
            String idempotencyKey,
            String requestFingerprint,
            ScrapeJobStatus status,
            String requestPayloadJson
    ) {
        this.jobId = jobId;
        this.userId = userId;
        this.portalType = portalType;
        this.operationType = operationType;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.status = status;
        this.requestPayloadJson = requestPayloadJson;
    }

    public static ScrapeJob createQueued(
            UUID userId,
            String portalType,
            ScrapeJobOperationType operationType,
            String idempotencyKey,
            String requestFingerprint,
            String requestPayloadJson
    ) {
        return new ScrapeJob(
                UUID.randomUUID().toString(),
                userId,
                portalType,
                operationType,
                idempotencyKey,
                requestFingerprint,
                ScrapeJobStatus.QUEUED,
                requestPayloadJson
        );
    }

    public boolean hasSameFingerprint(String requestFingerprint) {
        return this.requestFingerprint.equals(requestFingerprint);
    }

    public boolean isCompleted() {
        return status == ScrapeJobStatus.SUCCEEDED || status == ScrapeJobStatus.FAILED;
    }

    public boolean hasWorkerResult() {
        return resultPayloadJson != null;
    }

    public boolean hasProcessedAttempt(int attempt) {
        return callbackAttempt != null && attempt <= callbackAttempt;
    }

    public void markSucceeded(String resultPayloadJson, Instant finishedAt) {
        recordWorkerResult(resultPayloadJson, finishedAt);
        this.status = ScrapeJobStatus.SUCCEEDED;
    }

    public void recordWorkerResult(String resultPayloadJson, Instant finishedAt) {
        this.resultPayloadJson = resultPayloadJson;
        this.errorCode = null;
        this.errorMessage = null;
        this.retryable = null;
        this.finishedAt = finishedAt;
    }

    public void recordCallbackAttempt(int attempt, Instant receivedAt) {
        this.callbackAttempt = attempt;
        this.callbackReceivedAt = receivedAt;
    }

    public void recordResultLocation(String resultS3Key, int attempt, Instant receivedAt) {
        recordCallbackAttempt(attempt, receivedAt);
        this.resultS3Key = resultS3Key;
    }

    public void markFailed(String errorCode, String errorMessage, Boolean retryable, Instant finishedAt) {
        this.status = ScrapeJobStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
        this.finishedAt = finishedAt;
    }
}
