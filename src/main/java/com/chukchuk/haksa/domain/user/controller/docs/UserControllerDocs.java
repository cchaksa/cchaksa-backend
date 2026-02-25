package com.chukchuk.haksa.domain.user.controller.docs;

import com.chukchuk.haksa.domain.user.dto.UserDto;
import com.chukchuk.haksa.domain.user.wrapper.DeleteUserApiResponse;
import com.chukchuk.haksa.domain.user.wrapper.SignInApiResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "사용자 관련 API")
public interface UserControllerDocs {

    @Operation(
            summary = "회원 탈퇴",
            description = "로그인된 사용자의 계정을 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
                            content = @Content(schema = @Schema(implementation = DeleteUserApiResponse.class))),
                    @ApiResponse(responseCode = "404", description = "사용자 정보 없음 (ErrorCode: U01, USER_NOT_FOUND)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<MessageOnlyResponse>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "회원 가입 및 로그인",
            description = "사용자가 OIDC 소셜 로그인으로 회원가입 및 로그인을 진행합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "회원가입/로그인 성공",
                            content = @Content(schema = @Schema(implementation = SignInApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "토큰 유효성 오류 (ErrorCode: T10, TOKEN_INVALID)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "401", description = "만료된 토큰 오류 (ErrorCode: T04, TOKEN_EXPIRED)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    ResponseEntity<SuccessResponse<UserDto.SignInResponse>> signInUser(
            @RequestBody UserDto.SignInRequest signInRequest
    );
}
