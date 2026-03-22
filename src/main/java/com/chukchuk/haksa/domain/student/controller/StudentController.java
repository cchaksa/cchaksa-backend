package com.chukchuk.haksa.domain.student.controller;

import com.chukchuk.haksa.domain.student.controller.docs.StudentControllerDocs;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.chukchuk.haksa.domain.student.dto.StudentDto.StudentProfileResponse;
import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController implements StudentControllerDocs {

    private final StudentService studentService;

    @PostMapping("/target-gpa")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> setTargetGpa(
            @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) Double targetGpa
    ) {
        long t0 = LogTime.start();
        UUID studentId = studentService.getRequiredStudentIdByUserId(userDetails.getId());

        studentService.setStudentTargetGpa(studentId, targetGpa);

        long tookMs = LogTime.elapsedMs(t0);

        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] student.target_gpa.set.done took_ms={}", tookMs);
        }

        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("목표 학점 저장 완료")));
    }


    @GetMapping("/profile")
    public ResponseEntity<SuccessResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        UUID studentId = studentService.getRequiredStudentIdByUserId(userDetails.getId());

        StudentProfileResponse response = studentService.getStudentProfile(studentId);

        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] student.profile.get.done took_ms={}", tookMs);
        }

        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/reset")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetStudentData(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long t0 = LogTime.start();
        UUID studentId = studentService.getRequiredStudentIdByUserId(userDetails.getId());
        studentService.resetBy(studentId);
        long tookMs = LogTime.elapsedMs(t0);
        if (tookMs >= SLOW_MS) {
            log.info("[BIZ] student.reset.done took_ms={}", tookMs);
        }
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("학생 정보가 초기화되었습니다.")));
    }
}
