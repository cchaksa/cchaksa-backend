// SuwonScrapeControllerDocs.java
package com.chukchuk.haksa.application.api.docs;

import com.chukchuk.haksa.application.academic.wrapper.PortalLoginApiResponse;
import com.chukchuk.haksa.application.academic.wrapper.ScrapingApiResponse;
import com.chukchuk.haksa.application.dto.PortalLoginResponse;
import com.chukchuk.haksa.application.dto.ScrapingResponse;
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

public interface SuwonScrapeControllerDocs {

    @Operation(
            summary = "포털 로그인",
            description = "수원대학교 포털 로그인 후 Redis에 계정 정보를 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PortalLoginApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "로그인 실패 (ErrorCode: P01, 포털 로그인 실패)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류 (ErrorCode: INTERNAL_ERROR, 서버 오류)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<PortalLoginResponse>> login(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam @Parameter(description = "포털 로그인 ID", required = true) String username,
            @RequestParam @Parameter(description = "포털 로그인 비밀번호", required = true) String password
    );

    @Operation(
            summary = "포털 데이터 크롤링 및 동기화",
            description = "Redis에 저장된 포털 로그인 정보를 사용하여 데이터를 크롤링하고 초기화 및 학업 이력을 동기화합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "동기화 성공",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScrapingApiResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "아이디 또는 비밀번호 불일치 (ErrorCode: P01)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class))
                    ),
                    @ApiResponse(
                            responseCode = "423",
                            description = "계정 잠김 (ErrorCode: P03)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "포털 크롤링 실패 (ErrorCode: P02)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class))
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<ScrapingResponse>> startScraping(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "포털 정보 재연동 및 학업 이력 동기화",
            description = "포털 정보를 재연동하고 학업 이력을 다시 동기화합니다.",
            responses = {
                    @ApiResponse(responseCode = "202", description = "재연동 및 동기화 성공",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScrapingApiResponse.class))),
                    @ApiResponse(responseCode = "401", description = "로그인 필요 (ErrorCode: C01, 세션 만료)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class))),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류 (ErrorCode: C03, 재연동 실패)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseWrapper.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<SuccessResponse<ScrapingResponse>> refreshAndSync(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}