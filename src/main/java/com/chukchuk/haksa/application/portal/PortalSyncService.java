package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.domain.graduation.service.StudentGraduationProgressService;
import com.chukchuk.haksa.domain.student.model.Student;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSyncService {

    private final InitializePortalConnectionService initializePortalConnectionService;
    private final RefreshPortalConnectionService refreshPortalConnectionService;
    private final SyncAcademicRecordService syncAcademicRecordService;
    private final UserService userService;
    private final StudentService studentService;
    private final StudentGraduationProgressService studentGraduationProgressService;

    @Transactional
    public ScrapingResponse syncWithPortal(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();
        User mergedUser = userService.tryMergeWithExistingUser(userId, portalData.student().studentCode());
        UUID activeUserId = mergedUser.getId();
        if (Boolean.TRUE.equals(mergedUser.getPortalConnected())) {
            log.info("[BIZ] portal.sync.refresh_after_merge userId={} activeUserId={}", userId, activeUserId);
            return refreshActiveUserFromPortal(userId, mergedUser, portalData, t0);
        }

        // 1. 포털 초기화
        PortalConnectionResult conn = initializePortalConnectionService.executeWithPortalData(activeUserId, portalData);
        if (!conn.isSuccess()) {
            log.warn("[BIZ] portal.sync.conn.fail userId={} msg={}", activeUserId, LogSanitizer.arg(conn.error()));
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 2. 학업 이력 동기화
        SyncAcademicRecordResult sync = syncAcademicRecordService.executeWithPortalData(activeUserId, portalData);
        if (!sync.isSuccess()) {
            log.warn("[BIZ] portal.sync.sync.fail userId={} msg={}", activeUserId, LogSanitizer.arg(sync.getError()));
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 3. 외국어 졸업 인증 동기화
        syncLanguageCert(activeUserId, portalData);

        // 4. 포털 연결 마킹
        User user = userService.getUserById(activeUserId);
        user.markPortalConnected(Instant.now());
        userService.save(user);
        studentService.markReconnectedByUser(user);

        long tookMs = LogTime.elapsedMs(t0);
        log.info("[BIZ] portal.sync.done userId={} activeUserId={} took_ms={}", userId, activeUserId, tookMs);

        // 5. 응답 생성
        return ScrapingResponse.success(UUID.randomUUID().toString(), conn.studentInfo());
    }

    @Transactional
    public ScrapingResponse refreshFromPortal(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();
        User user = userService.getUserById(userId);
        return refreshActiveUserFromPortal(userId, user, portalData, t0);
    }

    private ScrapingResponse refreshActiveUserFromPortal(UUID userId, User activeUser, PortalData portalData, long t0) {
        UUID activeUserId = activeUser.getId();

        // 1. 포털 연동 정보 갱신
        PortalConnectionResult conn = refreshPortalConnectionService.executeWithPortalData(activeUserId, portalData);
        if (!conn.isSuccess()) {
            log.warn("[BIZ] portal.refresh.conn.fail userId={} msg={}", activeUserId, LogSanitizer.arg(conn.error()));
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 2. 학업 이력 재동기화
        SyncAcademicRecordResult sync = syncAcademicRecordService.executeForRefreshPortalData(activeUserId, portalData);
        if (!sync.isSuccess()) {
            log.warn("[BIZ] portal.refresh.sync.fail userId={} msg={}", activeUserId, LogSanitizer.arg(sync.getError()));
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 3. 외국어 졸업 인증 동기화
        syncLanguageCert(activeUserId, portalData);

        // 4. 마지막 동기화 시간만 업데이트 (포털 연결은 유지)
        activeUser.updateLastSyncedAt(Instant.now());
        userService.save(activeUser);
        studentService.markReconnectedByUser(activeUser);

        long tookMs = LogTime.elapsedMs(t0);
        log.info("[BIZ] portal.refresh.done userId={} activeUserId={} took_ms={}", userId, activeUserId, tookMs);

        // 5. 응답 생성
        return ScrapingResponse.success(UUID.randomUUID().toString(), conn.studentInfo());
    }

    private void syncLanguageCert(UUID activeUserId, PortalData portalData) {
        Student student = studentService.getStudentByUserId(activeUserId);
        studentGraduationProgressService.syncLanguageCert(
                student,
                portalData.student().languageCertFulfilled()
        );
    }
}
