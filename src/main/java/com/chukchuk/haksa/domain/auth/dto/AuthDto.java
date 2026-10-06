package com.chukchuk.haksa.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Builder;

/** 인증 토큰 발급과 갱신에 사용하는 요청·응답 형식을 묶는다. */
public class AuthDto {

  /**
   * 리프레시 토큰과 만료·세션 정보를 함께 전달한다.
   *
   * @param token 발급된 리프레시 토큰 원문
   * @param expiry 만료 시각
   * @param sessionId 세션 식별자
   */
  @Schema(description = "만료 시간을 포함한 리프레시 토큰 정보")
  public record RefreshTokenWithExpiry(
      @Schema(description = "Refresh Token", required = true) String token,
      @Schema(description = "만료 시각", required = true) Date expiry,
      @Schema(description = "로그인 세션 식별자", required = true) String sessionId) {
    /**
     * 세션 식별자 없이 리프레시 토큰 정보를 생성한다.
     *
     * @param token 리프레시 토큰
     * @param expiry 토큰 만료 시각
     */
    public RefreshTokenWithExpiry(String token, Date expiry) {
      this(token, expiry, null);
    }
  }

  /**
   * 갱신에 사용할 리프레시 토큰을 전달한다.
   *
   * @param refreshToken refresh token 원문
   */
  @Schema(description = "리프레시 토큰 요청 DTO")
  public record RefreshRequest(
      @Schema(description = "Refresh Token", required = true) String refreshToken) {}

  /**
   * 토큰 갱신으로 새로 발급된 액세스·리프레시 토큰을 전달한다.
   *
   * @param accessToken 새로 발급된 액세스 토큰
   * @param refreshToken refresh token 원문
   */
  @Schema(description = "Refresh Response DTO")
  public record RefreshResponse(
      @Schema(description = "액세스 토큰", required = true) String accessToken,
      @Schema(description = "리프레시 토큰", required = true) String refreshToken) {}

  /**
   * 로그인 성공으로 발급된 토큰과 포털 연동 상태를 전달한다.
   *
   * @param accessToken 로그인 후 발급된 액세스 토큰
   * @param refreshToken refresh token 원문
   * @param isPortalLinked 로그인 사용자의 포털 연동 완료 여부
   */
  @Schema(description = "카카오 로그인 성공 시 반환되는 토큰 정보")
  @Builder
  public record SignInTokenResponse(
      @Schema(description = "액세스 토큰", required = true) String accessToken,
      @Schema(description = "리프레시 토큰", required = true) String refreshToken,
      @Schema(description = "포털 연동 여부", example = "true", required = true)
          boolean isPortalLinked) {}
}
