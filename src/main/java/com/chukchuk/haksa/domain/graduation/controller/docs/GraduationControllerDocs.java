package com.chukchuk.haksa.domain.graduation.controller.docs;

import com.chukchuk.haksa.domain.graduation.dto.GraduationProgressResponse;
import com.chukchuk.haksa.domain.graduation.dto.LanguageCertRequirementResponse;
import com.chukchuk.haksa.domain.graduation.wrapper.GraduationProgressApiResponse;
import com.chukchuk.haksa.domain.graduation.wrapper.LanguageCertRequirementApiResponse;
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
            description = "로그인된 사용자의 졸업 요건 충족 여부와 외국어 졸업 인증 통과 여부를 조회합니다.",
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
                            description = "졸업 요건 정보 없음 (ErrorCode: G01, GRADUATION_REQUIREMENTS_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<GraduationProgressResponse>> getGraduationProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "외국어 인증 기준 조회",
            description = "로그인된 사용자의 학과 코드와 입학년도에 적용되는 외국어 인증 기준을 조회합니다. 미매핑 학과도 200 응답으로 반환됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "외국어 인증 기준 조회 성공",
                            content = @Content(schema = @Schema(implementation = LanguageCertRequirementApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "사용자 정보 없음 (ErrorCode: S01)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<LanguageCertRequirementResponse>> getLanguageCertRequirement(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
