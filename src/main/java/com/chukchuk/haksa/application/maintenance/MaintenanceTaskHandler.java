package com.chukchuk.haksa.application.maintenance;

import com.chukchuk.haksa.application.portal.ScrapeJobStaleReconciler;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceTaskHandler {

    private final ScrapeJobStaleReconciler scrapeJobStaleReconciler;
    private final RefreshTokenService refreshTokenService;

    public MaintenanceTaskResult handle(MaintenanceTaskRequest request) {
        long startedAt = System.nanoTime();
        MaintenanceTaskType taskType = MaintenanceTaskType.from(request.task());
        int affectedCount = switch (taskType) {
            case SCRAPE_JOB_RECONCILE_STALE -> scrapeJobStaleReconciler.reconcileStaleQueuedJobs();
            case REFRESH_TOKEN_CLEANUP -> refreshTokenService.deletedExpiredTokens();
        };

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("[BIZ] maintenance.task.completed task={} scheduledAt={} affectedCount={} elapsed_ms={}",
                taskType.name(), request.scheduledAt(), affectedCount, elapsedMs);
        return MaintenanceTaskResult.success(taskType.name(), affectedCount, request.scheduledAt());
    }
}
