package com.chukchuk.haksa.global.logging.policy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;
import java.util.Objects;

/**
 * HTTP 상태코드 → 로그 레벨 매핑
 * - 2xx/3xx: INFO
 * - 4xx: 기본 INFO, 단 401/403 반복·429·409·422: WARN
 * - 5xx: ERROR
 */
public class HttpStatusLevelMapper {

    public enum Level { INFO, WARN, ERROR }

    // 401/403 반복 감지 : 5분 윈도우 내 3회 이상
    private static final long WINDOW_MS = 5 * 60 * 1000L;
    private static final int THRESHOLD = 3;
    private static final int MAX_CACHE_SIZE = 10_000;
    private static final Cache<String, Counter> AUTH_FAILS = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMillis(WINDOW_MS))
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    private HttpStatusLevelMapper() {}

    public static Level map(int status, HttpServletRequest req) {
        if (status >= 500) return Level.ERROR;

        if (status == 401 || status == 403) {
            String key = key(req);
            Counter c = AUTH_FAILS.get(key, k -> new Counter());
            c.hit();
            return c.countInWindow(WINDOW_MS) >= THRESHOLD ? Level.WARN : Level.INFO;
        }

        if (status == 429 || status == 409 || status == 422) return Level.WARN;
        if (status >= 400) return Level.INFO;

        return Level.INFO; // 2xx/3xx
    }

    private static String key(HttpServletRequest req) {
        if (req == null) return "unknown|";
        String ip = firstForwardedFor(req.getHeader("X-Forwarded-For"));
        if (ip == null || ip.isBlank()) {
            String xReal = req.getHeader("X-Real-IP");
            ip = (xReal != null && !xReal.isBlank()) ? xReal : req.getRemoteAddr();
        }
        String path = Objects.toString(req.getRequestURI(), "");
        return (ip == null ? "unknown" : ip) + "|" + path;
    }

    private static String firstForwardedFor(String xff) {
        if (xff == null || xff.isBlank()) return null;
        int comma = xff.indexOf(',');
        return comma >= 0 ? xff.substring(0, comma).trim() : xff.trim();
    }

    /** 최근 호출 시간과 횟수를 관리하는 카운터 */
    private static class Counter {
        private volatile long last = 0L;
        private volatile int count = 0;

        synchronized void hit() {
            long now = System.currentTimeMillis();
            if (now - last > WINDOW_MS) count = 1;
            else count++;
            last = now;
        }

        synchronized int countInWindow(long windowMs) {
            return (System.currentTimeMillis() - last > windowMs) ? 0 : count;
        }
    }
}