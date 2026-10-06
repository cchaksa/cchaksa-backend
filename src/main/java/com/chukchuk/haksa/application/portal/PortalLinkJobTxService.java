package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutbox;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobStatus;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobOutboxRepository;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 포털 연동 작업과 발행 아웃박스를 하나의 트랜잭션에서 생성하거나 조회한다. */
@Service
@RequiredArgsConstructor
public class PortalLinkJobTxService {

  private final ScrapeJobRepository scrapeJobRepository;
  private final ScrapeJobOutboxRepository scrapeJobOutboxRepository;
  private final ObjectMapper objectMapper;

  /**
   * 멱등성 키에 해당하는 기존 작업을 재사용하거나 새 작업과 아웃박스를 함께 생성한다.
   *
   * @param userId 사용자 식별자
   * @param idempotencyKey 멱등성 키
   * @param portalType 포털 유형
   * @param operationType 작업 유형
   * @param requestFingerprint 동일 요청인지 판별할 요청 지문
   * @param requestPayloadJson 작업에 보관할 요청 JSON
   * @param username 포털 로그인 아이디
   * @param password 포털 비밀번호
   * @param requestedAt 작업 요청 시각
   * @return 작업·아웃박스 식별자와 재사용 및 즉시 발행 필요 여부
   * @throws CommonException 같은 멱등성 키의 요청 지문이 다르거나 기존 작업을 재사용할 수 없는 경우
   */
  @Transactional
  public PreparedJob createOrLoadJob(
      UUID userId,
      String idempotencyKey,
      String portalType,
      ScrapeJobOperationType operationType,
      String requestFingerprint,
      String requestPayloadJson,
      String username,
      String password,
      Instant requestedAt) {
    return scrapeJobRepository
        .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
        .map(existingJob -> toPreparedJob(existingJob, requestFingerprint))
        .orElseGet(
            () ->
                createNewJob(
                    userId,
                    idempotencyKey,
                    portalType,
                    operationType,
                    requestFingerprint,
                    requestPayloadJson,
                    username,
                    password,
                    requestedAt));
  }

  /**
   * 동시 생성 충돌 이후 멱등성 키에 해당하는 기존 작업과 아웃박스를 조회한다.
   *
   * @param userId 사용자 식별자
   * @param idempotencyKey 멱등성 키
   * @param requestFingerprint 기존 요청과 같은지 확인할 요청 지문
   * @return 기존 작업·아웃박스 식별자와 즉시 발행 필요 여부
   * @throws CommonException 작업 또는 아웃박스가 없거나 요청 지문이 다른 경우
   */
  @Transactional(readOnly = true)
  public PreparedJob loadExistingJob(
      UUID userId, String idempotencyKey, String requestFingerprint) {
    ScrapeJob existingJob =
        scrapeJobRepository
            .findByUserIdAndIdempotencyKey(userId, idempotencyKey)
            .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
    return toPreparedJob(existingJob, requestFingerprint);
  }

  /**
   * 동기 발행 후 작업과 아웃박스의 영속화 상태를 함께 조회한다.
   *
   * @param outboxId 아웃박스 식별자
   * @return 작업 상태, 아웃박스 상태, 큐 메시지 식별자 및 마지막 오류
   * @throws CommonException 작업 또는 아웃박스를 찾을 수 없는 경우
   */
  @Transactional(readOnly = true)
  public DispatchSnapshot loadDispatchSnapshot(String outboxId) {
    ScrapeJobOutbox outbox =
        scrapeJobOutboxRepository
            .findById(outboxId)
            .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
    ScrapeJob job =
        scrapeJobRepository
            .findById(outbox.getJobId())
            .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
    return new DispatchSnapshot(
        job.getJobId(),
        outbox.getOutboxId(),
        job.getStatus(),
        outbox.getStatus(),
        outbox.getQueueMessageId(),
        outbox.getLastError());
  }

  private PreparedJob createNewJob(
      UUID userId,
      String idempotencyKey,
      String portalType,
      ScrapeJobOperationType operationType,
      String requestFingerprint,
      String requestPayloadJson,
      String username,
      String password,
      Instant requestedAt) {
    ScrapeJob savedJob =
        scrapeJobRepository.save(
            ScrapeJob.createQueued(
                userId,
                portalType,
                operationType,
                idempotencyKey,
                requestFingerprint,
                requestPayloadJson,
                requestedAt));
    ScrapeJobOutbox savedOutbox =
        scrapeJobOutboxRepository.save(
            ScrapeJobOutbox.createPending(
                savedJob.getJobId(),
                buildOutboxPayload(
                    savedJob.getJobId(), userId, portalType, username, password, requestedAt),
                requestedAt));
    return PreparedJob.created(savedJob, savedOutbox);
  }

  private String buildOutboxPayload(
      String jobId,
      UUID userId,
      String portalType,
      String username,
      String password,
      Instant requestedAt) {
    try {
      return objectMapper.writeValueAsString(
          new ScrapeJobMessage(
              jobId,
              userId.toString(),
              portalType,
              new ScrapeJobMessage.RequestPayload(username, password),
              requestedAt.toString()));
    } catch (JsonProcessingException exception) {
      throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
    }
  }

  private PreparedJob toPreparedJob(ScrapeJob existingJob, String requestFingerprint) {
    if (!existingJob.hasSameFingerprint(requestFingerprint)) {
      throw new CommonException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    ScrapeJobOutbox outbox =
        scrapeJobOutboxRepository
            .findByJobId(existingJob.getJobId())
            .orElseThrow(() -> new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED));
    if (existingJob.getStatus() == ScrapeJobStatus.QUEUED
        && outbox.getStatus() == ScrapeJobOutboxStatus.DEAD) {
      throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED);
    }
    return PreparedJob.loaded(existingJob, outbox, requiresDispatch(existingJob, outbox));
  }

  private boolean requiresDispatch(ScrapeJob job, ScrapeJobOutbox outbox) {
    return job.getStatus() == ScrapeJobStatus.QUEUED
        && (outbox.getStatus() == ScrapeJobOutboxStatus.PENDING
            || outbox.getStatus() == ScrapeJobOutboxStatus.RETRYABLE_FAILED);
  }

  /**
   * 접수된 작업과 아웃박스의 후속 발행 판단 정보를 전달한다.
   *
   * @param jobId 작업 식별자
   * @param outboxId 아웃박스 식별자
   * @param reused 기존 작업을 재사용했는지 여부
   * @param dispatchRequired 현재 요청에서 아웃박스를 발행해야 하는지 여부
   */
  public record PreparedJob(
      String jobId, String outboxId, boolean reused, boolean dispatchRequired) {
    static PreparedJob created(ScrapeJob job, ScrapeJobOutbox outbox) {
      return new PreparedJob(job.getJobId(), outbox.getOutboxId(), false, true);
    }

    static PreparedJob loaded(ScrapeJob job, ScrapeJobOutbox outbox, boolean dispatchRequired) {
      return new PreparedJob(job.getJobId(), outbox.getOutboxId(), true, dispatchRequired);
    }
  }

  /**
   * 동기 발행 직후 확인한 작업과 아웃박스 상태를 전달한다.
   *
   * @param jobId 작업 식별자
   * @param outboxId 아웃박스 식별자
   * @param jobStatus 작업 상태
   * @param outboxStatus 아웃박스 상태
   * @param queueMessageId 큐 메시지 식별자
   * @param lastError 마지막 발행 실패 사유
   */
  public record DispatchSnapshot(
      String jobId,
      String outboxId,
      ScrapeJobStatus jobStatus,
      ScrapeJobOutboxStatus outboxStatus,
      String queueMessageId,
      String lastError) {
    /**
     * 작업과 outbox가 모두 전송 완료 상태인지 확인한다.
     *
     * @return outbox가 전송 완료이고 작업이 실행 중이면 {@code true}
     */
    public boolean isSent() {
      return outboxStatus == ScrapeJobOutboxStatus.SENT && jobStatus == ScrapeJobStatus.RUNNING;
    }
  }
}
