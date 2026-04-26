package com.chukchuk.haksa.application.maintenance;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum MaintenanceTaskType {
    SCRAPE_JOB_RECONCILE_STALE,
    REFRESH_TOKEN_CLEANUP;

    private static final Map<String, MaintenanceTaskType> LOOKUP = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(Enum::name, taskType -> taskType));

    public static MaintenanceTaskType from(String value) {
        MaintenanceTaskType taskType = LOOKUP.get(value);
        if (taskType == null) {
            throw new IllegalArgumentException("Unknown maintenance task: " + value);
        }
        return taskType;
    }
}
