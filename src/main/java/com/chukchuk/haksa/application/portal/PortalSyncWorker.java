package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.syncjob.JobStatus;
import com.chukchuk.haksa.domain.syncjob.JobType;
import com.chukchuk.haksa.domain.syncjob.SyncJob;
import com.chukchuk.haksa.domain.syncjob.SyncJobRepository;
import com.chukchuk.haksa.domain.syncjob.SyncPhase;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.repository.PortalRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

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
    @EventListener
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

    private PortalData fetchPortalData(Long jobId, UUID userId) {
        String key = userId.toString();
        String username = portalCredentialStore.getUsername(key);
        String password = portalCredentialStore.getPassword(key);

        if (username == null || password == null) {
            throw new CommonException(ErrorCode.SESSION_EXPIRED);
        }

        long t0 = LogTime.start();
        PortalData portalData = portalRepository.fetchPortalData(username, password);
        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[JOB] portal.fetch.done jobId={} userId={} took_ms={}", jobId, userId, tookMs);
        }

        portalCredentialStore.clear(key);
        return portalData;
    }

    //    private PortalData fetchPortalData(String userId) {
//        // 1. [재현] 7초 지연
//        try { Thread.sleep(7000); } catch (InterruptedException e) { }
//
//        // 2. [로드] 리소스 폴더의 파일 읽기 (상수 길이 제한 문제 해결)
//        PortalData originData;
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            // ClassLoader를 통해 리소스를 스트림으로 읽어오네.
//            InputStream is = getClass().getClassLoader().getResourceAsStream("mock_portal_data.json");
//            if (is == null) throw new RuntimeException("Mock JSON 파일을 찾을 수 없네!");
//            originData = objectMapper.readValue(is, PortalData.class);
//            } catch (IOException e) {
//            log.error("Failed to load mock data", e);
//            throw new RuntimeException(e);
//            }
//
//            // 3. [회피] 학번 랜덤화
//            String uniqueSno = String.valueOf(System.nanoTime()).substring(7, 15);
//
//            // 4. [교체] Record 불변 객체 새로 생성 (아까 짠 로직 그대로)
//            PortalStudentInfo origin = originData.student();
//            PortalStudentInfo randomizedStudent = new PortalStudentInfo(
//                uniqueSno,
//                origin.name(),
//                origin.college(),
//                origin.department(),
//                origin.major(),
//                origin.secondaryMajor(),
//                origin.status(),
//                origin.admission(),
//                origin.academic()
//                );
//        return new PortalData(randomizedStudent, originData.academic(), originData.curriculum());
//    }

    private void runPipeline(SyncJob job, PortalData portalData) {
        log.info("[JOB] portal.job.pipeline jobId={} type={}", job.getId(), job.getJobType());
        if (job.getJobType() == JobType.INITIAL_SYNC) {
            runInitialSync(job.getId(), job.getUserId(), portalData);
        } else {
            runRefreshSync(job.getId(), job.getUserId(), portalData);
        }
    }

    private void runInitialSync(Long jobId, UUID userId, PortalData portalData) {
        if (skipBecauseAlreadyConnected(jobId, userId, portalData)) {
            log.info("[JOB] portal.init.skip jobId={} userId={} reason=already_connected_after_merge", jobId, userId);
            return;
        }

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
