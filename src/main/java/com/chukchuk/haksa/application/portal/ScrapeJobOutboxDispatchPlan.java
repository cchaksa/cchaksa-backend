package com.chukchuk.haksa.application.portal;

import java.util.List;

public record ScrapeJobOutboxDispatchPlan(
        List<ScrapeJobOutboxPublishCandidate> candidates,
        int dispatchedCount
) {

    public static ScrapeJobOutboxDispatchPlan empty() {
        return new ScrapeJobOutboxDispatchPlan(List.of(), 0);
    }
}
