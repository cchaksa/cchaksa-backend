package com.chukchuk.haksa.domain.auth.controller.docs;

import static com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshRequest;
import static com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshResponse;

import com.chukchuk.haksa.domain.auth.wrapper.RefreshTokenApiResponse;
import com.chukchuk.haksa.global.common.response.SuccessResponse;
import com.chukchuk.haksa.global.common.response.wrapper.ErrorResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

/** 리프레시 토큰을 이용한 인증 토큰 재발급 API 계약을 정의한다. */
@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthControllerDocs {

  /**
   * 유효한 리프레시 토큰을 새 액세스·리프레시 토큰 쌍으로 교환한다.
   *
   * @param request 검증하고 교체할 리프레시 토큰
   * @return 재발급된 토큰 쌍을 담은 성공 응답
   */
  @Operation(
      summary = "토큰 재발급",
      description = "리프레시 토큰을 사용해 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "토큰 재발급 성공",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RefreshTokenApiResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패 (ErrorCode: T04, T10, T12, T11)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseWrapper.class))),
        @ApiResponse(
            responseCode = "404",
            description = "사용자 정보 없음 (ErrorCode: U01)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseWrapper.class)))
      })
  ResponseEntity<SuccessResponse<RefreshResponse>> refreshResponse(
      @RequestBody RefreshRequest request);
}
