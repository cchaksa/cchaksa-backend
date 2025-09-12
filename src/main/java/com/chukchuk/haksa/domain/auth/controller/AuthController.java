package com.chukchuk.haksa.domain.auth.controller;

import com.chukchuk.haksa.domain.auth.controller.docs.AuthControllerDocs;
import com.chukchuk.haksa.domain.auth.service.RefreshTokenService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshRequest;
import static com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshResponse;
import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerDocs {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<RefreshResponse>> refreshResponse(@RequestBody RefreshRequest request) {
        long t0 = LogTime.start();
        RefreshResponse response = refreshTokenService.reissue(request.refreshToken());
        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] auth.refresh.done took_ms={}", tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}