package com.chukchuk.haksa.domain.student.controller;

import com.chukchuk.haksa.domain.student.controller.docs.StudentControllerDocs;
import com.chukchuk.haksa.domain.student.service.StudentService;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.chukchuk.haksa.domain.student.dto.StudentDto.StudentProfileResponse;

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
        UUID studentId = userDetails.getStudentId();
        studentService.setStudentTargetGpa(studentId, targetGpa);
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("목표 학점 저장 완료")));
    }

    @GetMapping("/profile")
    public ResponseEntity<SuccessResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getStudentId();
        StudentProfileResponse response = studentService.getStudentProfile(studentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @PostMapping("/reset")
    public ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetStudentData(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UUID studentId = userDetails.getStudentId();
        studentService.resetBy(studentId);
        return ResponseEntity.ok(SuccessResponse.of(new MessageOnlyResponse("학생 정보가 초기화되었습니다.")));
    }
}