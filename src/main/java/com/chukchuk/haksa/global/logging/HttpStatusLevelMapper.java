package com.chukchuk.haksa.global.logging;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** HTTP 상태코드 → 로그 레벨 매핑 (401/403 반복, 429/409/422, 5xx 규칙 포함) */
public class HttpStatusLevelMapper {

    public enum Level { INFO, WARN, ERROR }

    // 간단한 메모리 카운터: 5분 내 401/403 반복 판단
    private static final Map<String, WindowCounter> AUTH_FAILS = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 5 * 60 * 1000L;
    private static final int THRESHOLD = 3;

    public static Level map(int status, HttpServletRequest req) {
        if (status >= 500) return Level.ERROR;

        if (status == 401 || status == 403) {
            String key = key(req);
            WindowCounter wc = AUTH_FAILS.computeIfAbsent(key, k -> new WindowCounter());
            wc.hit();
            if (wc.countInWindow(WINDOW_MS) >= THRESHOLD) return Level.WARN;
            return Level.INFO;
        }

        if (status == 429 || status == 409 || status == 422) return Level.WARN;
        if (status >= 400) return Level.INFO;
        return Level.INFO; // 2xx/3xx
    }

    private static String key(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        String path = req.getRequestURI();
        return ip + '|' + (path == null ? "" : path);
    }

    // ---- 내부 클래스: 윈도우 카운터 ----
    private static class WindowCounter {
        private volatile long last = 0L;
        private volatile int count = 0;

        synchronized void hit() {
            long now = System.currentTimeMillis();
            if (now - last > WINDOW_MS) {
                count = 1;
            } else {
                count++;
            }
            last = now;
        }

        synchronized int countInWindow(long windowMs) {
            if (System.currentTimeMillis() - last > windowMs) return 0;
            return count;
        }
    }
}