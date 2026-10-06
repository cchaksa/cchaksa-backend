package com.chukchuk.haksa.domain.academic.record.controller.docs;

import static com.chukchuk.haksa.domain.student.dto.StudentSemesterDto.StudentSemesterInfoResponse;

import com.chukchuk.haksa.domain.academic.record.dto.SemesterSummaryResponse;
import com.chukchuk.haksa.domain.academic.record.wrapper.SemesterGradesApiResponse;
import com.chukchuk.haksa.domain.academic.record.wrapper.StudentSemesterListApiResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import com.chukchuk.haksa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/** 인증 학생의 이수 학기 목록과 학기별 성적 목록 조회 API를 정의한다. */
public interface SemesterControllerDocs {

  /**
   * 로그인 학생이 이수한 학기별 성적 요약 목록을 조회한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @return 학생이 이수한 학기별 성적 요약 목록 성공 응답
   */
  @Operation(
      summary = "사용자 학기 목록 조회",
      description = "사용자의 모든 학기 정보를 조회합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "사용자의 모든 학기 정보 조회 성공",
            content =
                @Content(schema = @Schema(implementation = StudentSemesterListApiResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "학기 데이터 없음 (ErrorCode: A03, FRESHMAN_NO_SEMESTER)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<List<StudentSemesterInfoResponse>>> getSemesterRecord(
      @AuthenticationPrincipal CustomUserDetails userDetails);

  /**
   * 로그인 학생이 이수한 학기별 상세 성적 목록을 조회한다.
   *
   * @param userDetails 인증된 사용자 정보
   * @return 학생이 이수한 학기별 상세 성적 목록 성공 응답
   */
  @Operation(
      summary = "사용자 학기 별 성적 조회",
      description = "사용자의 학기 별 성적 정보를 조회합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "사용자의 학기 별 성적 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = SemesterGradesApiResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "학기 성적 데이터 없음 (ErrorCode: A02, SEMESTER_RECORD_EMPTY)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "500",
            description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR)",
            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  @SecurityRequirement(name = "bearerAuth")
  ResponseEntity<SuccessResponse<List<SemesterSummaryResponse>>> getSemesterGrades(
      @AuthenticationPrincipal CustomUserDetails userDetails);
}
