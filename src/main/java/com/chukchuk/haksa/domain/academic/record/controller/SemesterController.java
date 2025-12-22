package com.chukchuk.haksa.domain.academic.record.controller;

import com.chukchuk.haksa.domain.academic.record.controller.docs.SemesterControllerDocs;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.academic.record.service.SemesterAcademicRecordService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.chukchuk.haksa.domain.student.dto.StudentSemesterDto.StudentSemesterInfoResponse;
import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@RestController
@RequestMapping("/api/semester")
@RequiredArgsConstructor
public class SemesterController implements SemesterControllerDocs {

    private final SemesterAcademicRecordService semesterAcademicRecordService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<StudentSemesterInfoResponse>>> getSemesterRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long t0 = LogTime.start();

        UUID studentId = userDetails.getStudentId();

        List<StudentSemesterInfoResponse> response = semesterAcademicRecordService.getSemestersByStudentId(studentId);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            int count = (response != null) ? response.size() : 0;
            log.info("[BIZ] academic.semester.list.done studentId={} count={} took_ms={}", studentId, count, tookMs);
        }

        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/grades")
    public ResponseEntity<SuccessResponse<List<SemesterSummaryResponse>>> getSemesterGrades(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long t0 = LogTime.start();

        UUID studentId = userDetails.getStudentId();

        List<SemesterSummaryResponse> response = semesterAcademicRecordService.getAllSemesterGrades(studentId);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            int count = (response != null) ? response.size() : 0;
            log.info("[BIZ] academic.semester.grades.done studentId={} count={} took_ms={}",
                    studentId, count, tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
