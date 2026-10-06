package com.chukchuk.haksa.application.maintenance;

import com.chukchuk.haksa.application.portal.ScrapeJobStaleReconciler;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** EventBridge 유지보수 작업을 실제 정리 서비스로 분기하고 처리 결과를 기록한다. */
@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceTaskHandler {

  private final ScrapeJobStaleReconciler scrapeJobStaleReconciler;
  private final RefreshTokenService refreshTokenService;

  /**
   * 요청된 유지보수 작업을 실행하고 처리 건수와 실행 정보를 반환한다.
   *
   * @param request 실행할 작업 이름과 예약 시각을 담은 요청
   * @return 실행한 작업 이름과 처리 건수를 담은 결과
   * @throws IllegalArgumentException 지원하지 않는 작업 이름을 요청한 경우
   */
  public MaintenanceTaskResult handle(MaintenanceTaskRequest request) {
    long startedAt = System.nanoTime();
    MaintenanceTaskType taskType = MaintenanceTaskType.from(request.task());
    int affectedCount =
        switch (taskType) {
          case SCRAPE_JOB_RECONCILE_STALE -> scrapeJobStaleReconciler.reconcileStaleQueuedJobs();
          case REFRESH_TOKEN_CLEANUP -> refreshTokenService.deletedExpiredTokens();
        };

    long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
    log.info(
        "[BIZ] maintenance.task.completed task={} scheduledAt={} affectedCount={} elapsed_ms={}",
        taskType.name(),
        request.scheduledAt(),
        affectedCount,
        elapsedMs);
    return MaintenanceTaskResult.success(taskType.name(), affectedCount, request.scheduledAt());
  }
}
