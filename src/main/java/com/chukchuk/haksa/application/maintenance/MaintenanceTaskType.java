package com.chukchuk.haksa.application.maintenance;

import java.util.Arrays;

public enum MaintenanceTaskType {
    SCRAPE_JOB_RECONCILE_STALE,
    REFRESH_TOKEN_CLEANUP;

    public static MaintenanceTaskType from(String value) {
        return Arrays.stream(values())
                .filter(taskType -> taskType.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown maintenance task: " + value));
    }
}
