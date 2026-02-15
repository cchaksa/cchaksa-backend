package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.syncjob.*;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.model.PortalStudentInfo;
import com.chukchuk.haksa.infrastructure.portal.repository.PortalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortalSyncWorker {

    private final SyncJobRepository syncJobRepository;
    private final PortalCredentialStore portalCredentialStore;
    private final PortalRepository portalRepository;
    private final InitializePortalConnectionService initializePortalConnectionService;
    private final RefreshPortalConnectionService refreshPortalConnectionService;
    private final SyncAcademicRecordService syncAcademicRecordService;
    private final UserService userService;
    private final StudentService studentService;
    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 초기화 훅
     * 트랜잭션 전파 속성: PROPAGATION_REQUIRES_NEW 강제
     * - 항상 새로운 트랜잭션을 열어 작업을 독립적으로 수행
     */
    @PostConstruct
    void initTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate = template;
    }

    @Async("portalTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handlePortalSyncEvent(PortalSyncEvent event) {
        processJob(event.jobId());
    }

    public void processJob(Long jobId) {
        SyncJob job = claimJob(jobId);
        if (job == null) {
            log.debug("[JOB] portal.job.skipped jobId={} reason=unavailable", jobId);
            return;
        }

        log.info("[JOB] portal.job.phase jobId={} phase={}", jobId, SyncPhase.FETCHING);

        SyncPhase phase = SyncPhase.FETCHING;
        try {
            PortalData portalData = fetchPortalData(jobId, job.getUserId());
            phase = SyncPhase.SYNCING;
            log.info("[JOB] portal.job.phase jobId={} phase={}", jobId, SyncPhase.SYNCING);
            transition(jobId, JobStatus.PROCESSING, SyncPhase.SYNCING);
            runPipeline(job, portalData);
            transition(jobId, JobStatus.SUCCESS, SyncPhase.SYNCING);
            log.info("[JOB] portal.job.success jobId={} userId={} type={}", jobId, job.getUserId(), job.getJobType());
        } catch (Exception ex) {
            log.error("[JOB] portal.job.fail jobId={} userId={} type={} msg={}", jobId, job.getUserId(), job.getJobType(), ex.getMessage(), ex);
            transition(jobId, JobStatus.FAIL, phase);
        }
    }

    // 워커가 실제 작업을 시작하기 전에 선점하는 단계
    //
    private SyncJob claimJob(Long jobId) {
        return transactionTemplate.execute(status -> syncJobRepository.findByIdForUpdate(jobId)
                .filter(SyncJob::canProcess)
                .map(job -> {
                    job.updateStatus(JobStatus.PROCESSING, SyncPhase.FETCHING); // 트랜잭션 안이므로 DB 즉시 반영
                    return job;
                })
                .orElse(null));
    }

    private void transition(Long jobId, JobStatus status, SyncPhase phase) {
        transactionTemplate.executeWithoutResult(txStatus -> syncJobRepository.findById(jobId)
                .ifPresent(job -> job.updateStatus(status, phase)));
    }

//    private PortalData fetchPortalData(Long jobId, UUID userId) {
//        String key = userId.toString();
//        String username = portalCredentialStore.getUsername(key);
//        String password = portalCredentialStore.getPassword(key);
//
//        if (username == null || password == null) {
//            throw new CommonException(ErrorCode.SESSION_EXPIRED);
//        }
//
//        long t0 = LogTime.start();
//        PortalData portalData = portalRepository.fetchPortalData(username, password);
//        long tookMs = LogTime.elapsedMs(t0);
//        if (tookMs >= SLOW_MS) {
//            log.info("[JOB] portal.fetch.done jobId={} userId={} took_ms={}", jobId, userId, tookMs);
//        }
//
//        portalCredentialStore.clear(key);
//        return portalData;
//    }

        private PortalData fetchPortalData(Long jobId, UUID userId) {
        // 1. [재현] 7초 지연 (비동기 스레드만 멈추고 커넥션은 없는 상태여야 함)
        try { Thread.sleep(7000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            try (InputStream is = getClass().getClassLoader().getResourceAsStream("mock_portal_data.json")) {
                if (is == null) throw new RuntimeException("Mock JSON 파일을 찾을 수 없네!");

                PortalData originData = objectMapper.readValue(is, PortalData.class);

                // 2. [회피] 학번 랜덤화 (유일성 보장 강화)
                String uniqueSno = String.valueOf(System.nanoTime()).substring(7, 15);

                PortalStudentInfo origin = originData.student();
                PortalStudentInfo randomizedStudent = new PortalStudentInfo(
                        uniqueSno, // 랜덤 학번 주입
                        origin.name(),
                        origin.college(),
                        origin.department(),
                        origin.major(),
                        origin.secondaryMajor(),
                        origin.status(),
                        origin.admission(),
                        origin.academic()
                );

                return new PortalData(randomizedStudent, originData.academic(), originData.curriculum());
            } catch (IOException e) {
                log.error("[JOB] mock.load.fail jobId={}", jobId, e);
                throw new RuntimeException("Mock 데이터 로드 실패", e);
            }
    }

    private void runPipeline(SyncJob job, PortalData portalData) {
        log.info("[JOB] portal.job.pipeline jobId={} type={}", job.getId(), job.getJobType());
        if (job.getJobType() == JobType.INITIAL_SYNC) {
            runInitialSync(job.getId(), job.getUserId(), portalData);
        } else {
            runRefreshSync(job.getId(), job.getUserId(), portalData);
        }
    }

    private void runInitialSync(Long jobId, UUID userId, PortalData portalData) {
//        if (skipBecauseAlreadyConnected(jobId, userId, portalData)) {
//            log.info("[JOB] portal.init.skip jobId={} userId={} reason=already_connected_after_merge", jobId, userId);
//            return;
//        }

        PortalConnectionResult conn = initializePortalConnectionService.executeWithPortalData(userId, portalData);
        if (!conn.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        SyncAcademicRecordResult sync = syncAcademicRecordService.executeWithPortalData(userId, portalData);
        if (!sync.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        markUserConnected(jobId, userId);
    }

    private void runRefreshSync(Long jobId, UUID userId, PortalData portalData) {
        PortalConnectionResult conn = refreshPortalConnectionService.executeWithPortalData(userId, portalData);
        if (!conn.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        SyncAcademicRecordResult sync = syncAcademicRecordService.executeForRefreshPortalData(userId, portalData);
        if (!sync.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        markUserRefreshed(jobId, userId);
    }

    private boolean skipBecauseAlreadyConnected(Long jobId, UUID userId, PortalData portalData) {
        User mergedUser = userService.tryMergeWithExistingUser(userId, portalData.student().studentCode());
        boolean alreadyConnected = Boolean.TRUE.equals(mergedUser.getPortalConnected());
        if (alreadyConnected) {
            log.info("[JOB] portal.job.merged jobId={} userId={} mergedUserId={}", jobId, userId, mergedUser.getId());
        }
        return alreadyConnected;
    }

    private void markUserConnected(Long jobId, UUID userId) {
        User user = userService.getUserById(userId);
        user.markPortalConnected(Instant.now());
        userService.save(user);
        studentService.markReconnectedByUser(user);
        log.info("[JOB] portal.user.connected jobId={} userId={}", jobId, userId);
    }

    private void markUserRefreshed(Long jobId, UUID userId) {
        User user = userService.getUserById(userId);
        user.updateLastSyncedAt(Instant.now());
        userService.save(user);
        studentService.markReconnectedByUser(user);
        log.info("[JOB] portal.user.refreshed jobId={} userId={}", jobId, userId);
    }
}
