package com.chukchuk.haksa.domain.user.dto;

import com.chukchuk.haksa.global.security.service.OidcProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** 로그인, 내 정보 조회와 회원 탈퇴에 사용하는 요청·응답 형식을 묶는다. */
public class UserDto {

  /**
   * OIDC 제공자 토큰과 nonce를 포함한 로그인 요청을 표현한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param idToken OIDC 제공자가 발급한 ID 토큰
   * @param nonce OIDC 토큰 재사용을 방지할 로그인 요청 nonce
   */
  @Schema(description = "소셜 로그인 요청 정보")
  public record SignInRequest(
      @Schema(description = "OIDC Provider", example = "KAKAO", required = true)
          OidcProvider provider,
      @JsonProperty("id_token") @Schema(description = "OIDC Provider에서 발급받은 ID 토큰", required = true)
          String idToken,
      @Schema(description = "로그인 시 사용한 nonce 값", example = "random_nonce_value", required = true)
          String nonce) {}

  /**
   * 로그인 후 발급된 토큰과 포털 연동 여부를 표현한다.
   *
   * @param accessToken 로그인 후 발급된 액세스 토큰
   * @param refreshToken 발급된 리프레시 토큰 원문
   * @param isPortalLinked 로그인 사용자의 포털 연동 완료 여부
   */
  @Schema(description = "회원가입 및 로그인 응답")
  public record SignInResponse(
      @Schema(
              description = "Access Token",
              example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              required = true)
          String accessToken,
      @Schema(
              description = "Refresh Token",
              example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              required = true)
          String refreshToken,
      @Schema(description = "포털 연동 여부", example = "true", required = true)
          boolean isPortalLinked) {}

  /**
   * 외부 분석 도구에서 사용할 사용자 식별자를 표현한다.
   *
   * @param analyticsId 분석 도구에 전달할 사용자 식별자
   */
  @Schema(description = "사용자 분석 식별자 응답")
  public record AnalyticsIdResponse(
      @Schema(
              description = "Amplitude 사용자 식별자",
              example = "550e8400-e29b-41d4-a716-446655440000",
              required = true)
          String analyticsId) {}

  /**
   * 현재 사용자의 포털 연동 여부를 표현한다.
   *
   * @param isPortalLinked 현재 사용자의 포털 연동 완료 여부
   */
  @Schema(description = "내 사용자 정보 응답")
  public record MeResponse(
      @Schema(description = "포털 연동 여부", example = "true", required = true)
          boolean isPortalLinked) {}
}
