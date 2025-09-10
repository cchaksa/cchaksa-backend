package com.chukchuk.haksa.global.logging;

public final class LoggingThresholds {
    private LoggingThresholds() {}

    /** API 응답 지연 기준 (ms) */
    public static final long SLOW_MS = 500;

    /** 외부 연동 지연 기준 (ms) */
    public static final long EXT_SLOW_MS = 1000;

    /** DB 쿼리 지연 기준 (ms) */
    public static final long DB_SLOW_MS = 200;
}
