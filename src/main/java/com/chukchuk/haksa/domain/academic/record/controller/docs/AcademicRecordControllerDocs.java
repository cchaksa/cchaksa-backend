package com.chukchuk.haksa.domain.academic.record.controller.docs;

import com.chukchuk.haksa.domain.academic.record.dto.AcademicRecordResponse;
import com.chukchuk.haksa.domain.academic.record.wrapper.AcademicRecordApiResponse;
import com.chukchuk.haksa.domain.academic.record.wrapper.AcademicSummaryApiResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import static com.chukchuk.haksa.domain.academic.record.dto.StudentAcademicRecordDto.AcademicSummaryResponse;

public interface AcademicRecordControllerDocs {

    @Operation(
            summary = "학기별 성적 및 수강 과목 정보 조회",
            description = "지정한 학기(year, semester)에 해당하는 성적 및 수강 과목 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "지정 학기 성적 및 수강 과목 조회 성공",
                            content = @Content(schema = @Schema(implementation = AcademicRecordApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "404", description = "해당 학기 성적 데이터 없음 (A01)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<AcademicRecordResponse>> getAcademicRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @Parameter(description = "연도", example = "2024", required = true) Integer year,
            @RequestParam @Parameter(description = "학기", example = "10, 15, 20 ...", required = true) Integer semester
    );

    @Operation(
            summary = "사용자 학업 요약 정보 조회",
            description = "로그인된 사용자의 학업 요약 정보를 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "학업 요약 정보 조회 성공",
                            content = @Content(schema = @Schema(implementation = AcademicSummaryApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "404", description = "학업 요약 정보 없음 또는 사용자 정보 없음 (U02, S01)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "403", description = "포털 재연동 필요 (A07)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<AcademicSummaryResponse>> getAcademicSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}