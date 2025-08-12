package com.chukchuk.haksa.domain.academic.record.controller;

import com.chukchuk.haksa.domain.academic.record.controller.docs.SemesterControllerDocs;
import com.chukchuk.haksa.domain.academic.record.dto.SemesterAcademicRecordDto.SemesterGradeResponse;
import com.chukchuk.haksa.domain.academic.record.service.SemesterAcademicRecordService;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.chukchuk.haksa.domain.student.dto.StudentSemesterDto.StudentSemesterInfoResponse;

@LogPart
@RestController
@RequestMapping("/api/semester")
@RequiredArgsConstructor
public class SemesterController implements SemesterControllerDocs {

    private final SemesterAcademicRecordService semesterAcademicRecordService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<StudentSemesterInfoResponse>>> getSemesterRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID studentId = userDetails.getStudentId();
        List<StudentSemesterInfoResponse> response = semesterAcademicRecordService.getSemestersByStudentId(studentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }

    @GetMapping("/grades")
    public ResponseEntity<SuccessResponse<List<SemesterGradeResponse>>> getSemesterGrades(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID studentId = userDetails.getStudentId();
        List<SemesterGradeResponse> response = semesterAcademicRecordService.getAllSemesterGrades(studentId);
        return ResponseEntity.ok(SuccessResponse.of(response));
    }
}
