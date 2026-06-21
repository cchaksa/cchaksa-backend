package com.chukchuk.haksa.domain.lectureevaluations.controller.docs;

import com.chukchuk.haksa.domain.lectureevaluations.dto.LectureEvaluationDto;
import com.chukchuk.haksa.domain.lectureevaluations.wrapper.LectureEvaluationRequiredApiResponse;
import com.chukchuk.haksa.domain.lectureevaluations.wrapper.LectureEvaluationSkipApiResponse;
import com.chukchuk.haksa.domain.lectureevaluations.wrapper.LectureEvaluationSubmitApiResponse;
import com.chukchuk.haksa.global.common.response.MessageOnlyResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Lecture Evaluations", description = "강의평가 API")
public interface LectureEvaluationControllerDocs {

    @Operation(
            summary = "강의평가 상태 조회",
            description = "설정된 대상 학기의 강의평가 상태와 성적 카드 목록을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "강의평가 상태 조회 성공",
                            content = @Content(schema = @Schema(implementation = LectureEvaluationRequiredApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<LectureEvaluationDto.RequiredResponse>> getRequired(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "강의평가 제출",
            description = "설정된 대상 학기의 강의평가 데이터를 일괄 제출합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "강의평가 저장 성공",
                            content = @Content(schema = @Schema(implementation = LectureEvaluationSubmitApiResponse.class))),
                    @ApiResponse(responseCode = "400", description = "강의평가 대상 학기가 아니거나 제출 과목이 일치하지 않음",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LectureEvaluationDto.SubmitRequest request
    );

    @Operation(
            summary = "강의평가 건너뛰기",
            description = "설정된 대상 학기의 강의평가 상태를 SKIPPED로 변경합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "강의평가 건너뛰기 성공",
                            content = @Content(schema = @Schema(implementation = LectureEvaluationSkipApiResponse.class))),
                    @ApiResponse(responseCode = "400", description = "강의평가 대상 학기가 아니거나 pending 상태가 아님",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> skip(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LectureEvaluationDto.SkipRequest request
    );
}
