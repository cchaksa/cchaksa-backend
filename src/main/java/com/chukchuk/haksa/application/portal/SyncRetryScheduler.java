package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.domain.syncjob.JobStatus;
import com.chukchuk.haksa.domain.syncjob.SyncJob;
import com.chukchuk.haksa.domain.syncjob.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRetryScheduler {

    private final SyncJobRepository syncJobRepository;
    private final PortalSyncService portalSyncService;

    @Scheduled(fixedDelayString = "PT1M") // 스케줄러가 이전 실행이 끝난 뒤 1분마다 메서드 재호출
    public void recoverInitializedJobs() {
        // 생성된 지 5분이 넘은 INITIALIZED Job 찾기
        Instant cutoff = Instant.now().minus(5, ChronoUnit.MINUTES);
        List<SyncJob> staleJobs = syncJobRepository.findAllByStatusAndCreatedAtBefore(JobStatus.INITIALIZED, cutoff);
        if (staleJobs.isEmpty()) {
            return;
        }

        staleJobs.forEach(job -> {
            log.warn("[JOB] portal.job.retry jobId={} userId={} createdAt={}", job.getId(), job.getUserId(), job.getCreatedAt());
            portalSyncService.retry(job);
        });
    }
}
