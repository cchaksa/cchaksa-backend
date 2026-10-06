package com.chukchuk.haksa.domain.auth.service;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.entity.RefreshToken;
import com.chukchuk.haksa.domain.auth.repository.RefreshTokenRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 리프레시 토큰을 저장·검증·폐기하고 인증 토큰 재발급을 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProvider jwtProvider;
  private final UserRepository userRepository;
  private final RefreshTokenHasher refreshTokenHasher;

  @Value("${security.jwt.refresh-renewal-threshold:604800000}")
  private long refreshTokenRenewalThresholdMs = 604800000L;

  /* Refresh Token 저장 */
  /**
   * 세션별 리프레시 토큰 원문을 해시해 만료 시각과 함께 저장한다.
   *
   * @param sessionId 세션 식별자
   * @param userId 사용자 식별자
   * @param refreshToken 저장할 리프레시 토큰 원문
   * @param expiry 만료 시각
   */
  @Transactional
  public void save(String sessionId, String userId, String refreshToken, Date expiry) {
    RefreshToken token =
        new RefreshToken(sessionId, userId, null, refreshTokenHasher.hash(refreshToken), expiry);

    refreshTokenRepository.save(token);
  }

  /* 토큰 재발급 */
  /**
   * 유효한 refresh token으로 인증 토큰을 재발급한다.
   *
   * @param refreshToken refresh token 원문
   * @return 새 액세스 토큰과 리프레시 토큰
   * @throws TokenException 토큰이 유효하지 않거나 저장된 세션·해시와 일치하지 않는 경우
   * @throws EntityNotFoundException 토큰의 사용자 식별자에 해당하는 사용자가 없는 경우
   */
  @Transactional
  public AuthDto.RefreshResponse reissue(String refreshToken) {
    long t0 = LogTime.start();

    Claims claims = jwtProvider.parseToken(refreshToken);
    String userId = claims.getSubject();
    String sessionId = resolveSessionId(claims, userId);

    RefreshToken saved =
        refreshTokenRepository
            .findById(sessionId)
            .orElseThrow(
                () -> {
                  log.warn(
                      "[BIZ] auth.refresh.not_found userId={} sessionId={}", userId, sessionId);
                  return new TokenException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

    if (!matches(saved, refreshToken)) {
      log.warn("[BIZ] auth.refresh.mismatch userId={} sessionId={}", userId, sessionId);
      throw new TokenException(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }

    User user =
        userRepository
            .findById(UUID.fromString(userId))
            .orElseThrow(
                () -> {
                  log.warn("[BIZ] auth.refresh.user_not_found userId={}", userId);
                  return new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
                });

    String newAccessToken = jwtProvider.createAccessToken(userId, user.getEmail(), "USER");
    String responseRefreshToken = refreshToken;
    if (shouldRenewRefreshToken(saved.getExpiry())) {
      AuthDto.RefreshTokenWithExpiry newRefresh = jwtProvider.createRefreshToken(userId, sessionId);
      save(sessionId, userId, newRefresh.token(), newRefresh.expiry());
      responseRefreshToken = newRefresh.token();
    } else if (!saved.hasTokenHash()) {
      save(sessionId, userId, refreshToken, saved.getExpiry());
    }

    long tookMs = LogTime.elapsedMs(t0);
    if (tookMs >= SLOW_MS) {
      log.info("[BIZ] auth.refresh.issued userId={} took_ms={}", userId, tookMs);
    }
    return new AuthDto.RefreshResponse(newAccessToken, responseRefreshToken);
  }

  private boolean shouldRenewRefreshToken(Date expiry) {
    if (expiry == null) {
      return true;
    }
    long remainingMs = expiry.getTime() - System.currentTimeMillis();
    return remainingMs <= refreshTokenRenewalThresholdMs;
  }

  /**
   * 세션 식별자에 대응하는 리프레시 토큰을 조회한다.
   *
   * @param sessionId 세션 식별자
   * @return 세션 식별자에 해당하는 저장된 리프레시 토큰
   * @throws TokenException 세션 식별자에 해당하는 토큰이 없는 경우
   */
  public RefreshToken findBySessionId(String sessionId) {
    return refreshTokenRepository
        .findById(sessionId)
        .orElseThrow(() -> new TokenException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
  }

  /**
   * 사용자의 모든 refresh token을 폐기한다.
   *
   * @param userId 사용자 식별자
   */
  @Transactional
  public void deleteAllByUserId(String userId) {
    int deleted = refreshTokenRepository.deleteByUserId(userId);
    log.info("[BIZ] auth.refresh.delete_all userId={} count={}", userId, deleted);
  }

  /* 유효기간이 지난 RefreshToken 정보 삭제 */
  /**
   * 현재 시각보다 만료 시각이 이른 리프레시 토큰을 삭제한다.
   *
   * @return 삭제된 만료 토큰 수
   */
  @Transactional
  public int deletedExpiredTokens() {
    Date now = new Date();
    int deleted = refreshTokenRepository.deleteByExpiryBefore(now);
    log.info("[BIZ] auth.refresh.cleanup.deleted count={}", deleted);
    return deleted;
  }

  private String resolveSessionId(Claims claims, String userId) {
    String sessionId = claims.get("sid", String.class);
    if (sessionId == null || sessionId.isBlank()) {
      return userId;
    }
    return sessionId;
  }

  private boolean matches(RefreshToken saved, String refreshToken) {
    if (saved.hasTokenHash()) {
      return refreshTokenHasher.matches(refreshToken, saved.getTokenHash());
    }

    return saved.getToken() != null && saved.getToken().equals(refreshToken);
  }
}
