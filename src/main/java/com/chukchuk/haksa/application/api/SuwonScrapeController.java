package com.chukchuk.haksa.application.api;

import com.chukchuk.haksa.application.api.docs.SuwonScrapeControllerDocs;
import com.chukchuk.haksa.application.dto.PortalLoginResponse;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.application.portal.PortalSyncService;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.exception.CommonException;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import com.chukchuk.haksa.infrastructure.portal.exception.PortalScrapeException;
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

import java.util.UUID;

@LogPart("crawl")
@RestController
@RequestMapping("/api/suwon-scrape")
@RequiredArgsConstructor
@Tag(name = "Suwon Scraping", description = "수원대학교 포털 크롤링 관련 API")
@Slf4j
public class SuwonScrapeController implements SuwonScrapeControllerDocs {

    private final PortalRepository portalRepository;
    private final RedisPortalCredentialStore redisPortalCredentialStore;
    private final RedisCacheStore redisCacheStore;
    private final PortalSyncService portalSyncService;
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

        // 포털 연동 여부 사전 체크
        if (userService.getUserById(UUID.fromString(userId)).getPortalConnected()) {
            throw new CommonException(ErrorCode.USER_ALREADY_CONNECTED);
        }

        PortalData portalData = fetchPortalData(userId);

        ScrapingResponse response = portalSyncService.syncWithPortal(UUID.fromString(userId), portalData);

        // Redis 캐시 제거
        redisPortalCredentialStore.clear(userId);
        log.info("[COMPLETE] 동기화 완료: userId={}", userId); // 완료 로그

        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<ScrapingResponse>> refreshAndSync(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = userDetails.getUsername();
        log.info("[START] 포털 동기화 시작: userId={}", userId); // 요청 시작

        // 포털 연동 여부 사전 체크
        if (!userService.getUserById(UUID.fromString(userId)).getPortalConnected()) {
            throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
        }

        PortalData portalData = fetchPortalData(userId);

        ScrapingResponse response = portalSyncService.refreshFromPortal(UUID.fromString(userId), portalData);

        // 캐시 데이터 무효화
        UUID studentId = userDetails.getStudentId();

        redisCacheStore.deleteAllByStudentId(studentId);
        redisPortalCredentialStore.clear(userId);
        log.info("[COMPLETE] 동기화 완료: userId={}", userId); // 완료 로그

        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }

    private PortalData fetchPortalData(String userId) {
        String username = redisPortalCredentialStore.getUsername(userId); //학번
        String password = redisPortalCredentialStore.getPassword(userId);

        if (username == null || password == null) {
            throw new CommonException(ErrorCode.SESSION_EXPIRED);
        }

        try {
            log.info("[PORTAL] 포털 데이터 크롤링 시작: userId={}, username={}", userId, username);

            PortalData portalData = portalRepository.fetchPortalData(username, password);
            log.info("[PORTAL] 포털 데이터 크롤링 성공");
            return portalData;
        } catch (Exception e) {
            log.error("[PORTAL] 포털 데이터 크롤링 실패", e);
            throw new PortalScrapeException(ErrorCode.SCRAPING_FAILED);
        }
    }
}
