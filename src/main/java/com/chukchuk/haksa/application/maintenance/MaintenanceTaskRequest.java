package com.chukchuk.haksa.application.maintenance;

public record MaintenanceTaskRequest(
        String source,
        String task,
        String scheduledAt
) {
}
