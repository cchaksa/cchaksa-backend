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
        name = "scrape_job_outbox",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scrape_job_outbox_job_id", columnNames = {"job_id"})
        }
)
public class ScrapeJobOutbox extends BaseEntity {

    @Id
    @Column(name = "outbox_id", nullable = false, updatable = false)
    private String outboxId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private String jobId;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScrapeJobOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "queue_message_id")
    private String queueMessageId;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    private ScrapeJobOutbox(String outboxId, String jobId, String payloadJson, Instant nextAttemptAt) {
        this.outboxId = outboxId;
        this.jobId = jobId;
        this.payloadJson = payloadJson;
        this.status = ScrapeJobOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = nextAttemptAt;
    }

    public static ScrapeJobOutbox createPending(String jobId, String payloadJson, Instant nextAttemptAt) {
        return new ScrapeJobOutbox(
                UUID.randomUUID().toString(),
                jobId,
                payloadJson,
                nextAttemptAt
        );
    }

    public void markSent(String queueMessageId, Instant attemptedAt) {
        this.status = ScrapeJobOutboxStatus.SENT;
        this.attemptCount += 1;
        this.lastAttemptAt = attemptedAt;
        this.sentAt = attemptedAt;
        this.queueMessageId = queueMessageId;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void reserveForPublish(Instant reservedUntil, Instant attemptedAt) {
        this.nextAttemptAt = reservedUntil;
        this.lastAttemptAt = attemptedAt;
    }

    public void markRetryableFailure(String lastError, Instant attemptedAt, Instant nextAttemptAt) {
        this.status = ScrapeJobOutboxStatus.RETRYABLE_FAILED;
        this.attemptCount += 1;
        this.lastAttemptAt = attemptedAt;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
    }

    public void markDead(String lastError, Instant attemptedAt) {
        this.status = ScrapeJobOutboxStatus.DEAD;
        this.attemptCount += 1;
        this.lastAttemptAt = attemptedAt;
        this.nextAttemptAt = null;
        this.lastError = lastError;
    }
}
