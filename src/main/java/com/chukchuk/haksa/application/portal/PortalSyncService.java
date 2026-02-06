package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.domain.syncjob.JobType;
import com.chukchuk.haksa.domain.syncjob.SyncJob;
import com.chukchuk.haksa.domain.syncjob.SyncJobRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSyncService {

    private final SyncJobRepository syncJobRepository;
    private final PortalCredentialStore portalCredentialStore;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ScrapingResponse requestInitialSync(UUID userId) {
        ensureCredentialsPresent(userId);
        SyncJob job = syncJobRepository.save(SyncJob.create(userId, JobType.INITIAL_SYNC));
        publish(job);
        return pendingResponse(job);
    }

    @Transactional
    public ScrapingResponse requestRefreshSync(UUID userId) {
        ensureCredentialsPresent(userId);
        SyncJob job = syncJobRepository.save(SyncJob.create(userId, JobType.REFRESH_SYNC));
        publish(job);
        return pendingResponse(job);
    }

    public void retry(SyncJob job) {
        publish(job);
    }

    // job을 비동기 워커 큐에 보내는 역할
    private void publish(SyncJob job) {
        PortalSyncEvent event = new PortalSyncEvent(job.getId(), job.getUserId());
        eventPublisher.publishEvent(event); // 이벤트를 컨텍스트에 발행
        log.info("[JOB] portal.job.enqueued jobId={} userId={} type={}", job.getId(), job.getUserId(), job.getJobType());
    }

    private ScrapingResponse pendingResponse(SyncJob job) {
        return new ScrapingResponse(String.valueOf(job.getId()), null, job.getStatus().name());
    }

    // 사용자 포털 계정 정보가 캐시에 존재하는지 Validation, 비동기 워커 작업 최소 가드라인
    private void ensureCredentialsPresent(UUID userId) {
        String key = userId.toString();
        String username = portalCredentialStore.getUsername(key);
        String password = portalCredentialStore.getPassword(key);
        if (username == null || password == null) {
            throw new CommonException(ErrorCode.SESSION_EXPIRED);
        }
    }
}
