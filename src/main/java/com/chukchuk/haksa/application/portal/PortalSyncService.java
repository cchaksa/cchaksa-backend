package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
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

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalSyncService {

    private final InitializePortalConnectionService initializePortalConnectionService;
    private final RefreshPortalConnectionService refreshPortalConnectionService;
    private final SyncAcademicRecordService syncAcademicRecordService;
    private final UserService userService;
    private final StudentService studentService;

    @Transactional
    public ScrapingResponse syncWithPortal(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();

        // 1. 포털 초기화
        PortalConnectionResult conn = initializePortalConnectionService.executeWithPortalData(userId, portalData);
        if (!conn.isSuccess()) {
            log.warn("[BIZ] portal.sync.conn.fail userId={} msg={}", userId, LogSanitizer.arg(conn.error()));
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 2. 학업 이력 동기화
        SyncAcademicRecordResult sync = syncAcademicRecordService.executeWithPortalData(userId, portalData);
        if (!sync.isSuccess()) {
            log.warn("[BIZ] portal.sync.sync.fail userId={} msg={}", userId, LogSanitizer.arg(sync.getError()));
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 3. 포털 연결 마킹
        User user = userService.getUserById(userId);
        user.markPortalConnected(Instant.now());
        userService.save(user);
        studentService.markReconnectedByUser(user);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] portal.sync.done userId={} took_ms={}", userId, tookMs);
        }

        // 4. 응답 생성
        return new ScrapingResponse(UUID.randomUUID().toString(), conn.studentInfo());
    }

    @Transactional
    public ScrapingResponse refreshFromPortal(UUID userId, PortalData portalData) {
        long t0 = LogTime.start();

        // 1. 포털 연동 정보 갱신
        PortalConnectionResult conn = refreshPortalConnectionService.executeWithPortalData(userId, portalData);
        if (!conn.isSuccess()) {
            log.warn("[BIZ] portal.refresh.conn.fail userId={} msg={}", userId, LogSanitizer.arg(conn.error()));
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 2. 학업 이력 재동기화
        SyncAcademicRecordResult sync = syncAcademicRecordService.executeForRefreshPortalData(userId, portalData);
        if (!sync.isSuccess()) {
            log.warn("[BIZ] portal.refresh.sync.fail userId={} msg={}", userId, LogSanitizer.arg(sync.getError()));
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 3. 마지막 동기화 시간만 업데이트 (포털 연결은 유지)
        User user = userService.getUserById(userId);
        user.updateLastSyncedAt(Instant.now());
        userService.save(user);
        studentService.markReconnectedByUser(user);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] portal.refresh.done userId={} took_ms={}", userId, tookMs);
        }

        // 4. 응답 생성
        return new ScrapingResponse(UUID.randomUUID().toString(), conn.studentInfo());
    }

}