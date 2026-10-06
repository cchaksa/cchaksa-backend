// 스크래핑 callback의 작업 생성 시각 전달을 검증한다.

package com.chukchuk.haksa.application.portal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJob;
import com.chukchuk.haksa.domain.scrapejob.model.ScrapeJobOperationType;
import com.chukchuk.haksa.domain.scrapejob.repository.ScrapeJobRepository;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScrapeResultCallbackTxServiceTests {

  @Mock private ScrapeJobRepository scrapeJobRepository;

  @Mock private PortalSyncService portalSyncService;

  @Mock private PortalData portalData;

  @Test
  @DisplayName("성공 callback은 ScrapeJob 생성 시각을 LINK 동기화 버전으로 전달한다")
  void passesJobCreatedAtToLinkSync() {
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-08-30T02:00:00Z");
    ScrapeJob job =
        ScrapeJob.createQueued(
            userId, "suwon", ScrapeJobOperationType.LINK, "idempotency", "fingerprint", "{}");
    ReflectionTestUtils.setField(job, "createdAt", createdAt);
    when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
    ScrapeResultCallbackTxService service =
        new ScrapeResultCallbackTxService(
            scrapeJobRepository, portalSyncService, new SimpleMeterRegistry());

    service.completeSuccess(
        job.getJobId(),
        userId,
        ScrapeJobOperationType.LINK,
        portalData,
        "{}",
        Instant.parse("2026-08-30T02:01:00Z"),
        null,
        "hash");

    verify(portalSyncService).syncWithPortal(userId, portalData, createdAt);
  }

  @Test
  @DisplayName("성공 callback은 ScrapeJob 생성 시각을 REFRESH 동기화 버전으로 전달한다")
  void passesJobCreatedAtToRefreshSync() {
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-08-30T02:00:00Z");
    ScrapeJob job =
        ScrapeJob.createQueued(
            userId, "suwon", ScrapeJobOperationType.REFRESH, "idempotency", "fingerprint", "{}");
    ReflectionTestUtils.setField(job, "createdAt", createdAt);
    when(scrapeJobRepository.findForUpdateByJobId(job.getJobId())).thenReturn(Optional.of(job));
    ScrapeResultCallbackTxService service =
        new ScrapeResultCallbackTxService(
            scrapeJobRepository, portalSyncService, new SimpleMeterRegistry());

    service.completeSuccess(
        job.getJobId(),
        userId,
        ScrapeJobOperationType.REFRESH,
        portalData,
        "{}",
        Instant.parse("2026-08-30T02:01:00Z"),
        null,
        "hash");

    verify(portalSyncService).refreshFromPortal(eq(userId), eq(portalData), eq(createdAt));
  }
}
