package com.chukchuk.haksa.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Date;


public class AuthDto {

    @Schema(description = "만료 시간을 포함한 리프레시 토큰 정보")
    public record RefreshTokenWithExpiry(
            @Schema(description = "Refresh Token", required = true)
            String token,

            @Schema(description = "만료 시각", required = true)
            Date expiry
    ) {}

    @Schema(description = "리프레시 토큰 요청 DTO")
    public record RefreshRequest(
            @Schema(description = "Refresh Token", required = true)
            String refreshToken
    ) {}

    @Schema(description = "Refresh Response DTO")
    public record RefreshResponse(
            @Schema(description = "액세스 토큰", required = true)
            String accessToken,

            @Schema(description = "리프레시 토큰", required = true)
            String refreshToken
    ) {}

    @Schema(description = "카카오 로그인 성공 시 반환되는 토큰 정보")
    @Builder
    public record SignInTokenResponse(
            @Schema(description = "액세스 토큰", required = true)
            String accessToken,

            @Schema(description = "리프레시 토큰", required = true)
            String refreshToken,

            @Schema(description = "포털 연동 여부", example = "true", required = true)
            boolean isPortalLinked
    ) {}

    @Schema(description = "CSRF 토큰 응답 DTO")
    public record CsrfTokenResponse(
            @Schema(description = "CSRF 토큰 값", required = true)
            String token,

            @Schema(description = "클라이언트가 토큰을 전달해야 하는 헤더 이름", example = "X-XSRF-TOKEN", required = true)
            String headerName
    ) {}
}
