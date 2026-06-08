package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;

import java.util.UUID;

public record ScrapeJobOutboxPublishCandidate(
        String outboxId,
        String jobId,
        UUID userId,
        ScrapeJobOperationType operationType,
        String payloadJson,
        int attemptCount,
        ScrapeJobOutboxStatus status,
        String queueMessageId
) {
}
