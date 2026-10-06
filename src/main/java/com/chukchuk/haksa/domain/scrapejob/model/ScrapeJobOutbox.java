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

/** 스크래핑 작업 메시지의 발행 상태와 재시도 정보를 작업과 함께 보관한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "scrape_job_outbox",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_scrape_job_outbox_job_id",
          columnNames = {"job_id"})
    })
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

  private ScrapeJobOutbox(
      String outboxId, String jobId, String payloadJson, Instant nextAttemptAt) {
    this.outboxId = outboxId;
    this.jobId = jobId;
    this.payloadJson = payloadJson;
    this.status = ScrapeJobOutboxStatus.PENDING;
    this.attemptCount = 0;
    this.nextAttemptAt = nextAttemptAt;
  }

  /**
   * 다음 발행 시각이 지정된 대기 아웃박스를 생성한다.
   *
   * @param jobId 작업 식별자
   * @param payloadJson JSON payload
   * @param nextAttemptAt 다음 발행 시도 시각
   * @return 첫 발행 시각과 payload가 설정된 대기 아웃박스
   */
  public static ScrapeJobOutbox createPending(
      String jobId, String payloadJson, Instant nextAttemptAt) {
    return new ScrapeJobOutbox(UUID.randomUUID().toString(), jobId, payloadJson, nextAttemptAt);
  }

  /**
   * 아웃박스 발행 성공과 연결된 작업 상태를 기록한다.
   *
   * @param queueMessageId 큐 메시지 식별자
   * @param attemptedAt 발행 시도 시각
   */
  public void markSent(String queueMessageId, Instant attemptedAt) {
    this.status = ScrapeJobOutboxStatus.SENT;
    this.attemptCount += 1;
    this.lastAttemptAt = attemptedAt;
    this.sentAt = attemptedAt;
    this.queueMessageId = queueMessageId;
    this.nextAttemptAt = null;
    this.lastError = null;
  }

  /**
   * 아웃박스를 발행 예약 상태로 전환한다.
   *
   * @param reservedUntil 발행 예약이 유지되는 시각
   * @param attemptedAt 발행 시도 시각
   */
  public void reserveForPublish(Instant reservedUntil, Instant attemptedAt) {
    this.nextAttemptAt = reservedUntil;
    this.lastAttemptAt = attemptedAt;
  }

  /**
   * 아웃박스 발행 실패와 다음 재시도 시각을 기록한다.
   *
   * @param lastError 마지막 발행 실패 원인
   * @param attemptedAt 발행 시도 시각
   * @param nextAttemptAt 다음 발행 시도 시각
   */
  public void markRetryableFailure(String lastError, Instant attemptedAt, Instant nextAttemptAt) {
    this.status = ScrapeJobOutboxStatus.RETRYABLE_FAILED;
    this.attemptCount += 1;
    this.lastAttemptAt = attemptedAt;
    this.nextAttemptAt = nextAttemptAt;
    this.lastError = lastError;
  }

  /**
   * 재시도하지 않을 아웃박스 실패를 기록한다.
   *
   * @param lastError 마지막 발행 실패 원인
   * @param attemptedAt 발행 시도 시각
   */
  public void markDead(String lastError, Instant attemptedAt) {
    this.status = ScrapeJobOutboxStatus.DEAD;
    this.attemptCount += 1;
    this.lastAttemptAt = attemptedAt;
    this.nextAttemptAt = null;
    this.lastError = lastError;
  }
}
