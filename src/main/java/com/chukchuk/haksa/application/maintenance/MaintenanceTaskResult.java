package com.chukchuk.haksa.application.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaintenanceTaskResult(
        boolean success,
        String task,
        @JsonProperty("affected_count")
        int affectedCount,
        @JsonProperty("scheduled_at")
        String scheduledAt
) {

    public static MaintenanceTaskResult success(String task, int affectedCount, String scheduledAt) {
        return new MaintenanceTaskResult(true, task, affectedCount, scheduledAt);
    }
}
