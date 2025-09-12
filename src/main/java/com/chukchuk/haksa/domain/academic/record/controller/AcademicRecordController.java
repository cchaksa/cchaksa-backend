package com.chukchuk.haksa.domain.academic.record.controller;

import com.chukchuk.haksa.domain.academic.record.controller.docs.AcademicRecordControllerDocs;
import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.service.AcademicRecordService;
import com.chukchuk.haksa.domain.academic.record.service.StudentAcademicRecordService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;
import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@LogPart
@Slf4j
@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class AcademicRecordController implements AcademicRecordControllerDocs {

    private final AcademicRecordService academicRecordService;
    private final StudentAcademicRecordService studentAcademicRecordService;

    @GetMapping("/record")
    public ResponseEntity<SuccessResponse<AcademicRecordResponse>> getAcademicRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer year,
            @RequestParam Integer semester) {

        long t0 = LogTime.start();
        UUID studentId = userDetails.getStudentId();

        AcademicRecordResponse response = academicRecordService.getAcademicRecord(studentId, year, semester);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] academic.record.done studentId={} year={} semester={} took_ms={}",
                    studentId, year, semester, tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<SuccessResponse<AcademicSummaryResponse>> getAcademicSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        long t0 = LogTime.start();
        UUID studentId = userDetails.getStudentId();

        AcademicSummaryResponse response = studentAcademicRecordService.getAcademicSummary(studentId);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] academic.summary.done studentId={} took_ms={}", studentId, tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
