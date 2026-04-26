package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOutboxStatus;

public record ScrapeJobOutboxPublishCandidate(
        String outboxId,
        String jobId,
        String payloadJson,
        int attemptCount,
        ScrapeJobOutboxStatus status,
        String queueMessageId
) {
}
