package com.chukchuk.haksa.application.api;

import com.chukchuk.haksa.application.api.docs.SuwonScrapeControllerDocs;
import com.chukchuk.haksa.application.dto.PortalLoginResponse;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
import com.chukchuk.haksa.application.portal.PortalSyncService;
import com.chukchuk.haksa.domain.cache.AcademicCache;
import com.chukchuk.haksa.domain.portal.PortalCredentialStore;
import com.chukchuk.haksa.domain.user.service.UserService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.global.security.CustomUserDetails;
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

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@RestController
@RequestMapping("/api/suwon-scrape")
@RequiredArgsConstructor
@Tag(name = "Suwon Scraping", description = "수원대학교 포털 크롤링 관련 API")
@Slf4j
public class SuwonScrapeController implements SuwonScrapeControllerDocs {

    private final PortalCredentialStore portalCredentialStore;
    private final AcademicCache academicCache;
    private final PortalSyncService portalSyncService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<PortalLoginResponse>> login(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String username,
            @RequestParam String password
    ) {
        String userId = userDetails.getUsername();
        portalCredentialStore.save(userId, username, password);

        return ResponseEntity.ok(SuccessResponse.of(new PortalLoginResponse("로그인 성공")));
    }

    @PostMapping("/start")
    public ResponseEntity<SuccessResponse<ScrapingResponse>> startScraping(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        String userId = userDetails.getUsername();

        // 포털 연동 여부 사전 체크 - 조건 위반만 WARN
        if (userService.getUserById(UUID.fromString(userId)).getPortalConnected()) {
            log.warn("[BIZ] portal.start skipped userId={} reason=already_connected", userId);
            throw new CommonException(ErrorCode.USER_ALREADY_CONNECTED);
        }

        ScrapingResponse response = portalSyncService.requestInitialSync(UUID.fromString(userId));

        long tookMs = LogTime.elapsedMs(t0);
        // 처리가 느린 경우만 INFO
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] portal.done userId={} taskId={} took_ms={}",
                    userId, LogSanitizer.clean(response.taskId()), tookMs);
        }

        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<ScrapingResponse>> refreshAndSync(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        String userId = userDetails.getUsername();

        // 포털 연동 여부 사전 체크
        if (!userService.getUserById(UUID.fromString(userId)).getPortalConnected()) {
            log.warn("[BIZ] portal.refresh.skipped userId={} reason=not_connected", userId);
            throw new CommonException(ErrorCode.USER_NOT_CONNECTED);
        }

        ScrapingResponse response = portalSyncService.requestRefreshSync(UUID.fromString(userId));

        // 캐시 데이터 초기화
        UUID studentId = userDetails.getStudentId();
        academicCache.deleteAllByStudentId(studentId);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] portal.refresh.done userId={} took_ms={}", userId, tookMs);
        }

        return ResponseEntity.accepted().body(SuccessResponse.of(response));
    }
}
