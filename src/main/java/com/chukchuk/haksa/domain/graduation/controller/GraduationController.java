package com.chukchuk.haksa.domain.graduation.controller;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

import com.chukchuk.haksa.domain.graduation.controller.docs.GraduationControllerDocs;
import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.domain.graduation.service.GraduationService;
import com.chukchuk.haksa.domain.graduation.service.LanguageCertRequirementService;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 졸업 HTTP 요청을 처리한다. */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/graduation")
public class GraduationController implements GraduationControllerDocs {

  private final GraduationService graduationService;
  private final LanguageCertRequirementService languageCertRequirementService;
  private final StudentService studentService;

  @Override
  @GetMapping("/progress")
  public ResponseEntity<SuccessResponse<GraduationProgressResponse>> getGraduationProgress(
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    long t0 = LogTime.start();
    UUID studentId = studentService.getRequiredStudentIdByUserId(userDetails.getId());
    GraduationProgressResponse response = graduationService.getGraduationProgress(studentId);
    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info("[BIZ] graduation.progress.done took_ms={}", tookMs);
    }
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @Override
  @GetMapping("/language-cert/requirement")
  public ResponseEntity<SuccessResponse<LanguageCertRequirementResponse>>
      getLanguageCertRequirement(@AuthenticationPrincipal CustomUserDetails userDetails) {
    UUID studentId = studentService.getRequiredStudentIdByUserId(userDetails.getId());
    LanguageCertRequirementResponse response =
        languageCertRequirementService.getRequirement(studentId);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }
}
