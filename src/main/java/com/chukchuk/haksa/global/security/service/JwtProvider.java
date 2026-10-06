package com.chukchuk.haksa.global.security.service;

import static com.chukchuk.haksa.domain.auth.dto.AuthDto.RefreshTokenWithExpiry;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 사용자 인증용 access token과 세션 식별자가 포함된 refresh token을 발급·검증한다. */
@Component
@Slf4j
public class JwtProvider {

  @Value("${security.jwt.secret}")
  private String secret;

  @Value("${security.jwt.access-expiration}")
  private long accessTokenExpiration; // ms 단위

  @Value("${security.jwt.refresh-expiration}")
  private long refreshTokenExpiration; // ms 단위

  private Key key;

  /** JWT 서명 키를 초기화하고 유효성을 검사한다. */
  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes()); // base64 아님!
  }

  // AccessToken 토큰 생성
  /**
   * 사용자 식별자·이메일·권한을 claim으로 담은 access token을 발급한다.
   *
   * @param userId 사용자 식별자
   * @param email 인증 사용자 이메일 claim
   * @param role 인가에 사용할 사용자 역할 claim
   * @return 설정된 access token 만료 시간이 적용된 서명 JWT
   */
  public String createAccessToken(String userId, String email, String role) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
        .setSubject(userId)
        .claim("email", email)
        .claim("role", role)
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  // RefreshToken 생성
  /**
   * 임의의 새 세션 식별자로 refresh token을 발급한다.
   *
   * @param userId 사용자 식별자
   * @return refresh token 원문, 만료 시각 및 생성된 세션 식별자
   */
  public RefreshTokenWithExpiry createRefreshToken(String userId) {
    return createRefreshToken(userId, UUID.randomUUID().toString());
  }

  // RefreshToken 생성
  /**
   * 지정한 세션 식별자를 {@code sid} claim으로 담은 refresh token을 발급한다.
   *
   * @param userId 사용자 식별자
   * @param sessionId 세션 식별자
   * @return refresh token 원문, 만료 시각 및 전달받은 세션 식별자
   */
  public RefreshTokenWithExpiry createRefreshToken(String userId, String sessionId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + refreshTokenExpiration);

    String token =
        Jwts.builder()
            .setSubject(userId)
            .claim("sid", sessionId)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

    return new RefreshTokenWithExpiry(token, expiry, sessionId);
  }

  // 토큰 검증
  /**
   * JWT 서명과 만료 시각을 검증하고 claim을 반환한다.
   *
   * @param token 검증하고 해석할 JWT 문자열
   * @return 서명이 유효한 JWT claim
   * @throws ExpiredJwtException 허용된 clock skew를 포함해 토큰이 만료된 경우
   * @throws JwtException 토큰 형식이나 서명이 유효하지 않은 경우
   */
  public Claims parseToken(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(key)
          .setAllowedClockSkewSeconds(60)
          .build()
          .parseClaimsJws(token) // 서명 유효성 검증 + Base64 디코딩, Claims 추출
          .getBody();
    } catch (ExpiredJwtException e) {
      throw e;
    } catch (Exception e) {
      log.info("JWT parsing error: {}", e.getMessage(), e);
      throw new JwtException("Invalid token", e);
    }
  }
}
