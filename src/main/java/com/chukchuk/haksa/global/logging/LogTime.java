package com.chukchuk.haksa.global.logging;

import java.time.Duration;

public final class LogTime {
    private LogTime() {}

    /** 현재 시각 (ns) */
    public static long start() {
        return System.nanoTime();
    }

    /** 경과 시간 (ms) */
    public static long elapsedMs(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
