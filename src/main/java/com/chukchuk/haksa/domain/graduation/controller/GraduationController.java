package com.chukchuk.haksa.domain.graduation.controller;

import com.chukchuk.haksa.domain.graduation.controller.docs.GraduationControllerDocs;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.service.GraduationService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/graduation")
public class GraduationController implements GraduationControllerDocs {

    private final GraduationService graduationService;

    @GetMapping("/progress")
    public ResponseEntity<SuccessResponse<GraduationProgressResponse>> getGraduationProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        UUID studentId = userDetails.getStudentId();
        GraduationProgressResponse response = graduationService.getGraduationProgress(studentId);
        long tookMS = LogTime.elapsedMs(t0);
        if (tookMS >= SLOW_MS) {
            log.info("[BIZ] graduation.progress.done took_ms={}", tookMS);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}