package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.portal.dto.PortalLinkDto;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** 포털 연동 요청을 멱등하게 접수하고 스크래핑 작업 발행을 조정한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortalLinkJobService {

  private final PortalLinkJobTxService portalLinkJobTxService;
  private final ScrapeJobOutboxDispatcher scrapeJobOutboxDispatcher;
  private final UserService userService;
  private final ObjectMapper objectMapper;

  /**
   * 포털 연동 작업을 접수하고 비동기 처리 정보를 반환한다.
   *
   * @param userId 사용자 식별자
   * @param idempotencyKey 같은 요청을 재사용하기 위한 비어 있지 않은 멱등성 키
   * @param request 포털 유형과 자격 증명을 담은 요청
   * @return 접수되거나 재사용된 작업 식별자와 상태 조회 경로
   * @throws CommonException 요청이 유효하지 않거나 멱등성 충돌 또는 작업 발행에 실패한 경우
   */
  public PortalLinkDto.AcceptedResponse acceptJob(
      UUID userId, String idempotencyKey, PortalLinkDto.LinkRequest request) {
    validateRequest(idempotencyKey, request);

    User user = userService.getUserById(userId);
    ScrapeJobOperationType operationType =
        Boolean.TRUE.equals(user.getPortalConnected())
            ? ScrapeJobOperationType.REFRESH
            : ScrapeJobOperationType.LINK;

    String requestFingerprint =
        createRequestFingerprint(
            request.portalType(), request.username(), request.password(), operationType);
    String requestPayloadJson = toRequestPayloadJson(request.username(), request.password());
    Instant requestedAt = Instant.now();

    try {
      PortalLinkJobTxService.PreparedJob preparedJob =
          portalLinkJobTxService.createOrLoadJob(
              userId,
              idempotencyKey,
              request.portalType(),
              operationType,
              requestFingerprint,
              requestPayloadJson,
              request.username(),
              request.password(),
              requestedAt);
      return finalizeAcceptedJob(preparedJob, userId, request, operationType, idempotencyKey);
    } catch (DataIntegrityViolationException exception) {
      PortalLinkJobTxService.PreparedJob preparedJob =
          portalLinkJobTxService.loadExistingJob(userId, idempotencyKey, requestFingerprint);
      return finalizeAcceptedJob(preparedJob, userId, request, operationType, idempotencyKey);
    } catch (CommonException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.warn(
          "[BIZ] scrape.job.enqueue.fail userId={} portalType={} idempotencyKey={}",
          userId,
          request.portalType(),
          idempotencyKey,
          exception);
      throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
    }
  }

  /**
   * 포털 계정과 작업 유형을 정규화해 멱등성 비교용 SHA-256 지문을 생성한다.
   *
   * <p>포털 유형은 앞뒤 공백과 대소문자를 무시하고, 아이디는 앞뒤 공백을 제거한다. 비밀번호와 작업 유형은 원문을 사용한다.
   *
   * @param portalType 포털 유형
   * @param username null이 아닌 포털 로그인 아이디
   * @param password 포털 비밀번호
   * @param operationType 작업 유형
   * @return 정규화된 요청을 소문자 16진수로 표현한 SHA-256 해시
   * @throws IllegalStateException 입력을 정규화하거나 SHA-256 지문을 생성할 수 없는 경우
   */
  public static String createRequestFingerprint(
      String portalType, String username, String password, ScrapeJobOperationType operationType) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String canonical =
          String.join("|", normalize(portalType), username.trim(), password, operationType.name());
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to create request fingerprint", exception);
    }
  }

  private static String normalize(String portalType) {
    return portalType == null ? "" : portalType.trim().toLowerCase();
  }

  private void validateRequest(String idempotencyKey, PortalLinkDto.LinkRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
    if (request.username() == null
        || request.username().isBlank()
        || request.password() == null
        || request.password().isBlank()) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT);
    }
    if (!"suwon".equals(normalize(request.portalType()))) {
      throw new CommonException(ErrorCode.UNSUPPORTED_PORTAL_TYPE);
    }
  }

  private String toRequestPayloadJson(String username, String password) {
    try {
      return objectMapper.writeValueAsString(
          new ScrapeJobMessage.RequestPayload(username, password));
    } catch (JsonProcessingException exception) {
      throw new CommonException(ErrorCode.INVALID_ARGUMENT, exception);
    }
  }

  private PortalLinkDto.AcceptedResponse finalizeAcceptedJob(
      PortalLinkJobTxService.PreparedJob preparedJob,
      UUID userId,
      PortalLinkDto.LinkRequest request,
      ScrapeJobOperationType operationType,
      String idempotencyKey) {
    if (preparedJob.dispatchRequired()) {
      dispatchSynchronously(preparedJob, request.portalType(), idempotencyKey);
    }

    if (preparedJob.reused()) {
      log.info(
          "[BIZ] scrape.job.idempotent.reuse jobId={} userId={} portalType={} "
              + "operationType={} idempotencyKey={}",
          preparedJob.jobId(),
          userId,
          request.portalType(),
          operationType,
          idempotencyKey);
    } else {
      log.info(
          "[BIZ] scrape.job.accepted jobId={} userId={} portalType={} "
              + "operationType={} idempotencyKey={}",
          preparedJob.jobId(),
          userId,
          request.portalType(),
          operationType,
          idempotencyKey);
    }
    return new PortalLinkDto.AcceptedResponse(
        preparedJob.jobId(), "accepted", "/portal/link/jobs/" + preparedJob.jobId());
  }

  private void dispatchSynchronously(
      PortalLinkJobTxService.PreparedJob preparedJob, String portalType, String idempotencyKey) {
    try {
      scrapeJobOutboxDispatcher.dispatchOnce(preparedJob.outboxId());
      PortalLinkJobTxService.DispatchSnapshot snapshot =
          portalLinkJobTxService.loadDispatchSnapshot(preparedJob.outboxId());
      if (!snapshot.isSent()) {
        log.warn(
            "[BIZ] scrape.job.enqueue.sync.fail jobId={} outboxId={} portalType={} "
                + "idempotencyKey={} jobStatus={} outboxStatus={} queueMessageId={} "
                + "lastError={}",
            snapshot.jobId(),
            snapshot.outboxId(),
            portalType,
            idempotencyKey,
            snapshot.jobStatus(),
            snapshot.outboxStatus(),
            snapshot.queueMessageId(),
            snapshot.lastError());
        throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED);
      }
    } catch (CommonException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      log.warn(
          "[BIZ] scrape.job.enqueue.sync.exception jobId={} outboxId={} "
              + "portalType={} idempotencyKey={}",
          preparedJob.jobId(),
          preparedJob.outboxId(),
          portalType,
          idempotencyKey,
          exception);
      throw new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED, exception);
    }
  }
}
