package com.chukchuk.haksa.domain.auth.service;

import com.chukchuk.haksa.domain.auth.dto.AuthDto;
import com.chukchuk.haksa.domain.auth.entity.RefreshToken;
import com.chukchuk.haksa.domain.auth.repository.RefreshTokenRepository;
import com.chukchuk.haksa.domain.user.model.User;
import com.chukchuk.haksa.domain.user.repository.UserRepository;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.TokenException;
import com.chukchuk.haksa.global.logging.annotation.LogTime;
import com.chukchuk.haksa.global.security.service.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

import static com.chukchuk.haksa.global.logging.config.LoggingThresholds.SLOW_MS;

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
    @Transactional
    public void save(String sessionId, String userId, String refreshToken, Date expiry) {
        RefreshToken token = new RefreshToken(sessionId, userId, null, refreshTokenHasher.hash(refreshToken), expiry);

        refreshTokenRepository.save(token);
    }

    /* 토큰 재발급 */
    @Transactional
    public AuthDto.RefreshResponse reissue(String refreshToken) {
        long t0 = LogTime.start();

        Claims claims = jwtProvider.parseToken(refreshToken);
        String userId = claims.getSubject();
        String sessionId = resolveSessionId(claims, userId);

        RefreshToken saved = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> {
                    log.warn("[BIZ] auth.refresh.not_found userId={} sessionId={}", userId, sessionId);
                    return new TokenException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        if (!matches(saved, refreshToken)) {
            log.warn("[BIZ] auth.refresh.mismatch userId={} sessionId={}", userId, sessionId);
            throw new TokenException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> {
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

    public RefreshToken findBySessionId(String sessionId) {
        return refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new TokenException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    /* 유효기간이 지난 RefreshToken 정보 삭제 */
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
