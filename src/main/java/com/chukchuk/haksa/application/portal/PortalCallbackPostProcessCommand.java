package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;

import java.time.Instant;
import java.util.UUID;

public record PortalCallbackPostProcessCommand(
        String jobId,
        UUID userId,
        ScrapeJobOperationType operationType,
        String resultPayloadJson,
        Instant finishedAt,
        Double queuedAgeSeconds
) {
}
