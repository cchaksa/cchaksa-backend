package com.chukchuk.haksa.domain.graduation.controller.docs;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.wrapper.GraduationProgressApiResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Graduation", description = "졸업 요건 및 진행 현황 관련 API")
public interface GraduationControllerDocs {

    @Operation(
            summary = "졸업 요건 진행 상황 조회",
            description = "로그인된 사용자의 졸업 요건 충족 여부를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "졸업 요건 충족 여부 조회 성공",
                            content = @Content(schema = @Schema(implementation = GraduationProgressApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자 정보 없음 (ErrorCode: S01, FRESHMAN_NO_SEMESTER)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "졸업 요건 정보 없음 (ErrorCode: G02, GRADUATION_REQUIREMENTS_DATA_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
                    ),
                    @ApiResponse(
                            responseCode = "422",
                            description = "편입생 학적 정보는 현재 미지원 (ErrorCode: T13, TRANSFER_STUDENT_UNSUPPORTED)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<GraduationProgressResponse>> getGraduationProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}