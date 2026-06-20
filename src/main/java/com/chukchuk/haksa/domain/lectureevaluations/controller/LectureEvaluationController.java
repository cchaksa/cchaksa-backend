package com.chukchuk.haksa.domain.lectureevaluations.controller;

import com.chukchuk.haksa.domain.lectureevaluations.controller.docs.LectureEvaluationControllerDocs;
import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.domain.lectureevaluations.service.LectureEvaluationService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@RestController
@RequestMapping("/api/lecture-evaluations")
@RequiredArgsConstructor
public class LectureEvaluationController implements LectureEvaluationControllerDocs {

    private final LectureEvaluationService lectureEvaluationService;

    @GetMapping("/required")
    public ResponseEntity<SuccessResponse<LectureEvaluationDto.RequiredResponse>> getRequired(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        LectureEvaluationDto.RequiredResponse response = lectureEvaluationService.getRequired(userDetails.getId());
        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] lecture_evaluation.required.get.done userId={} took_ms={}",
                    userDetails.getId(), tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LectureEvaluationDto.SubmitRequest request
    ) {
        long t0 = LogTime.start();
        lectureEvaluationService.submit(userDetails.getId(), request);
        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] lecture_evaluation.submit.done userId={} year={} semester={} count={} took_ms={}",
                    userDetails.getId(), request.year(), request.semester(), request.evaluations().size(), tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("강의평가 저장 완료")));
    }
}
