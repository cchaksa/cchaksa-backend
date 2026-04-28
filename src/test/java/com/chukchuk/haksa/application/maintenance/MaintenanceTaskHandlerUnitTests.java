package com.chukchuk.haksa.application.maintenance;

import com.chukchuk.haksa.application.portal.ScrapeJobStaleReconciler;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceTaskHandlerUnitTests {

    @Mock
    private ScrapeJobStaleReconciler scrapeJobStaleReconciler;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("SCRAPE_JOB_RECONCILE_STALE 작업은 stale reconciler를 실행하고 처리 건수를 반환한다")
    void handle_reconcileStale_returnsAffectedCount() {
        MaintenanceTaskHandler handler = new MaintenanceTaskHandler(scrapeJobStaleReconciler, refreshTokenService);
        when(scrapeJobStaleReconciler.reconcileStaleQueuedJobs()).thenReturn(2);

        MaintenanceTaskResult result = handler.handle(new MaintenanceTaskRequest(
                "eventbridge.scheduler",
                "SCRAPE_JOB_RECONCILE_STALE",
                "2026-04-26T00:00:00Z"
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.task()).isEqualTo("SCRAPE_JOB_RECONCILE_STALE");
        assertThat(result.affectedCount()).isEqualTo(2);
        assertThat(result.scheduledAt()).isEqualTo("2026-04-26T00:00:00Z");
        verify(scrapeJobStaleReconciler).reconcileStaleQueuedJobs();
    }

    @Test
    @DisplayName("REFRESH_TOKEN_CLEANUP 작업은 만료 토큰 정리를 실행하고 삭제 건수를 반환한다")
    void handle_refreshTokenCleanup_returnsDeletedCount() {
        MaintenanceTaskHandler handler = new MaintenanceTaskHandler(scrapeJobStaleReconciler, refreshTokenService);
        when(refreshTokenService.deletedExpiredTokens()).thenReturn(3);

        MaintenanceTaskResult result = handler.handle(new MaintenanceTaskRequest(
                "eventbridge.scheduler",
                "REFRESH_TOKEN_CLEANUP",
                "2026-04-26T00:00:00Z"
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.task()).isEqualTo("REFRESH_TOKEN_CLEANUP");
        assertThat(result.affectedCount()).isEqualTo(3);
        assertThat(result.scheduledAt()).isEqualTo("2026-04-26T00:00:00Z");
        verify(refreshTokenService).deletedExpiredTokens();
    }

    @Test
    @DisplayName("알 수 없는 maintenance task는 실패한다")
    void handle_unknownTask_throws() {
        MaintenanceTaskHandler handler = new MaintenanceTaskHandler(scrapeJobStaleReconciler, refreshTokenService);

        assertThatThrownBy(() -> handler.handle(new MaintenanceTaskRequest(
                "eventbridge.scheduler",
                "UNKNOWN_TASK",
                "2026-04-26T00:00:00Z"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN_TASK");
    }
}
