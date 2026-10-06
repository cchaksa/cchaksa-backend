package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import java.util.UUID;

/**
 * 큐 발행에 필요한 스크래핑 작업 아웃박스의 현재 상태와 payload를 전달한다.
 *
 * @param outboxId 아웃박스 식별자
 * @param jobId 작업 식별자
 * @param userId 사용자 식별자
 * @param operationType 작업 유형
 * @param payloadJson 큐에 전송할 JSON payload
 * @param attemptCount 지금까지의 발행 시도 횟수
 * @param status 예약 시점의 아웃박스 상태
 * @param queueMessageId 이전 발행에서 받은 큐 메시지 식별자
 */
public record ScrapeJobOutboxPublishCandidate(
    String outboxId,
    String jobId,
    UUID userId,
    ScrapeJobOperationType operationType,
    String payloadJson,
    int attemptCount,
    ScrapeJobOutboxStatus status,
    String queueMessageId) {}
