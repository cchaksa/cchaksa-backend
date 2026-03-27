package com.chukchuk.haksa.domain.user.dto;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserDto {

    @Schema(description = "소셜 로그인 요청 정보")
    public record SignInRequest(
            @Schema(description = "OIDC Provider", example = "KAKAO", required = true)
            OidcProvider provider,

            @Schema(description = "OIDC Provider에서 발급받은 ID 토큰", required = true)
            String id_token,

            @Schema(description = "로그인 시 사용한 nonce 값", example = "random_nonce_value", required = true)
            String nonce
    ) {}

    @Schema(description = "회원가입 및 로그인 응답")
    public record SignInResponse(

            @Schema(description = "Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
            String accessToken,

            @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", required = true)
            String refreshToken,

            @Schema(description = "포털 연동 여부", example = "true", required = true)
            boolean isPortalLinked

    ) {}
}
