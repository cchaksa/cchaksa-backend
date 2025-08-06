package com.chukchuk.haksa.domain.student.controller.docs;

import com.chukchuk.haksa.domain.student.wrapper.StudentProfileApiResponse;
import com.chukchuk.haksa.domain.student.wrapper.TargetGpaApiResponse;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import static com.chukchuk.haksa.domain.student.dto.StudentDto.StudentProfileResponse;

@Tag(name = "Student", description = "학생 설정 관련 API")
public interface StudentControllerDocs {

    @Operation(
            summary = "목표 GPA 설정",
            description = "로그인된 사용자의 목표 GPA를 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "목표 GPA 설정 성공",
                            content = @Content(schema = @Schema(implementation = TargetGpaApiResponse.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 GPA 입력 (ErrorCode: S02, INVALID_TARGET_GPA)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "404", description = "학생 정보 없음 (ErrorCode: S01, STUDENT_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> setTargetGpa(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false)
            @Parameter(description = "목표 GPA", example = "3.8") Double targetGpa
    );

    @Operation(
            summary = "사용자 프로필 조회",
            description = "로그인된 사용자의 프로필 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "사용자 프로필 정보 조회 성공",
                            content = @Content(schema = @Schema(implementation = StudentProfileApiResponse.class))),
                    @ApiResponse(responseCode = "404", description = "학생 정보 없음 (ErrorCode: S01, STUDENT_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<StudentProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "사용자 정보 초기화",
            description = "로그인된 사용자의 정보를 초기화합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> resetStudentData(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}