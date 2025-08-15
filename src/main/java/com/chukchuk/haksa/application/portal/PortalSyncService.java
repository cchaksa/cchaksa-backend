package com.chukchuk.haksa.application.portal;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

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
        // 1. 포털 초기화
        PortalConnectionResult portalConnectionResult = initializePortalConnectionService.executeWithPortalData(userId, portalData);
        if (!portalConnectionResult.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 2. 학업 이력 동기화
        SyncAcademicRecordResult syncResult = syncAcademicRecordService.executeWithPortalData(userId, portalData);
        if (!syncResult.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 3. 포털 연결 마킹
        User user = userService.getUserById(userId);
        user.markPortalConnected(Instant.now());
        userService.save(user);

        studentService.markReconnectedByUserId(userId);

        // 4. 응답 생성
        return new ScrapingResponse(UUID.randomUUID().toString(), portalConnectionResult.studentInfo());
    }

    @Transactional
    public ScrapingResponse refreshFromPortal(UUID userId, PortalData portalData) {
        // 1. 포털 연동 정보 갱신
        PortalConnectionResult portalConnectionResult = refreshPortalConnectionService.executeWithPortalData(userId, portalData);
        if (!portalConnectionResult.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        // 2. 학업 이력 재동기화
        SyncAcademicRecordResult syncResult = syncAcademicRecordService.executeForRefreshPortalData(userId, portalData);
        if (!syncResult.isSuccess()) {
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 3. 마지막 동기화 시간만 업데이트 (포털 연결은 유지)
        User user = userService.getUserById(userId);
        user.updateLastSyncedAt(Instant.now());
        userService.save(user);

        studentService.markReconnectedByUserId(userId);

        // 4. 응답 생성
        return new ScrapingResponse(UUID.randomUUID().toString(), portalConnectionResult.studentInfo());
    }

}