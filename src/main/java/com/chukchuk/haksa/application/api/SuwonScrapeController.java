package com.chukchuk.haksa.application.api;

import com.chukchuk.haksa.application.academic.dto.SyncAcademicRecordResult;
import com.chukchuk.haksa.application.api.docs.SuwonScrapeControllerDocs;
import com.chukchuk.haksa.application.dto.PortalLoginResponse;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.application.portal.InitializePortalConnectionService;
import com.chukchuk.haksa.application.portal.RefreshPortalConnectionService;
import com.chukchuk.haksa.application.portal.SyncAcademicRecordService;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
import com.chukchuk.haksa.infrastructure.portal.model.PortalConnectionResult;
import com.chukchuk.haksa.infrastructure.portal.model.PortalData;
import com.chukchuk.haksa.infrastructure.portal.repository.PortalRepository;
import com.chukchuk.haksa.infrastructure.redis.RedisCacheStore;
import com.chukchuk.haksa.infrastructure.redis.RedisPortalCredentialStore;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@LogPart("crawl")
@RestController
@RequestMapping("/api/suwon-scrape")
@RequiredArgsConstructor
@Tag(name = "Suwon Scraping", description = "수원대학교 포털 크롤링 관련 API")
@Slf4j
public class SuwonScrapeController implements SuwonScrapeControllerDocs {

    private final PortalRepository portalRepository;
    private final InitializePortalConnectionService initializePortalConnectionService;
    private final RefreshPortalConnectionService refreshPortalConnectionService;
    private final SyncAcademicRecordService syncAcademicRecordService;
    private final RedisPortalCredentialStore redisPortalCredentialStore;
    private final RedisCacheStore redisCacheStore;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<PortalLoginResponse>> login(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String username,
            @RequestParam String password
    ) {
        String userId = userDetails.getUsername();
        redisPortalCredentialStore.save(userId, username, password);

        return ResponseEntity.ok(SuccessResponse.of(new PortalLoginResponse("로그인 성공")));
    }

    @PostMapping("/start")
    public ResponseEntity<SuccessResponse<ScrapingResponse>> startScraping(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = userDetails.getUsername();
        log.info("[START] 포털 동기화 시작: userId={}", userId); // 요청 시작

        String[] credentials = loadPortalCredentials(userId);
        String username = credentials[0];
        String password = credentials[1];

        PortalData portalData;
        try {
            portalData = portalRepository.fetchPortalData(username, password);
            log.info("[PORTAL] 포털 데이터 크롤링 성공");
        } catch (Exception e) {
            log.error("[PORTAL] 포털 데이터 크롤링 실패", e);
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        UUID uuUserId = UUID.fromString(userId);
        PortalConnectionResult portalConnectionResult = initializePortalConnectionService.executeWithPortalData(uuUserId, portalData);
        if (!portalConnectionResult.isSuccess()) {
            log.warn("[INIT] 포털 초기화 실패: userId={}, reason={}", userId, portalConnectionResult.error());
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        SyncAcademicRecordResult syncResult = syncAcademicRecordService.executeWithPortalData(uuUserId, portalData);
        if (!syncResult.isSuccess()) {
            log.warn("[SYNC] 학업 동기화 실패: userId={}, reason={}", userId, syncResult.getError());
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        redisPortalCredentialStore.clear(userId);
        log.info("[COMPLETE] 동기화 완료: userId={}", userId); // 완료 로그

        // 사용자 포털 연결 정보 설정
        User user = userService.getUserById(uuUserId);
        user.markPortalConnected(Instant.now());
        userService.save(user);


        ScrapingResponse response = new ScrapingResponse(UUID.randomUUID().toString(), portalConnectionResult.studentInfo());
        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<ScrapingResponse>> refreshAndSync(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = userDetails.getUsername();
        log.info("[START] 포털 동기화 시작: userId={}", userId); // 요청 시작

        String[] credentials = loadPortalCredentials(userId);
        String username = credentials[0];
        String password = credentials[1];

        PortalData portalData;
        try {
            portalData = portalRepository.fetchPortalData(username, password);
            log.info("[PORTAL] 포털 데이터 크롤링 성공");
        } catch (Exception e) {
            log.error("[PORTAL] 포털 데이터 크롤링 실패", e);
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        UUID uuUserId = UUID.fromString(userId);
        PortalConnectionResult portalConnectionResult = refreshPortalConnectionService.executeWithPortalData(uuUserId, portalData);
        if (!portalConnectionResult.isSuccess()) {
            log.warn("[INIT] 포털 초기화 실패: userId={}, reason={}", userId, portalConnectionResult.error());
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }

        SyncAcademicRecordResult syncResult = syncAcademicRecordService.executeForRefreshPortalData(uuUserId, portalData);
        if (!syncResult.isSuccess()) {
            log.warn("[SYNC] 학업 동기화 실패: userId={}, reason={}", userId, syncResult.getError());
            throw new PortalScrapeException(ErrorCode.REFRESH_FAILED);
        }

        // 캐시 데이터 무효화
        UUID studentId = userDetails.getStudentId();
        redisCacheStore.deleteAllByStudentId(studentId);

        redisPortalCredentialStore.clear(userId);
        log.info("[COMPLETE] 동기화 완료: userId={}", userId); // 완료 로그

        User user = userService.getUserById(uuUserId);
        user.updateLastSyncedAt(Instant.now());
        userService.save(user);

        ScrapingResponse response = new ScrapingResponse(UUID.randomUUID().toString(), portalConnectionResult.studentInfo());
        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }

    private String[] loadPortalCredentials(String userId) {
        String username = redisPortalCredentialStore.getUsername(userId);
        String password = redisPortalCredentialStore.getPassword(userId);
        if (username == null || password == null) {
            throw new CommonException(ErrorCode.SESSION_EXPIRED);
        }
        return new String[]{username, password};
    }
}
