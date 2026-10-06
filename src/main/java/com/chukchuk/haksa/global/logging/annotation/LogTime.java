package com.chukchuk.haksa.global.logging.annotation;

import java.time.Duration;

/** 메서드 실행 시간 로깅 대상을 표시한다. */
public final class LogTime {
  private LogTime() {}

  /**
   * 경과 시간 측정을 시작할 단조 증가 시각을 반환한다.
   *
   * @return {@link System#nanoTime()} 기준 시작 시각
   */
  public static long start() {
    return System.nanoTime();
  }

  /**
   * 시작 시각부터 현재까지 경과한 시간을 밀리초로 반환한다.
   *
   * @param startNanos {@link #start()}로 얻은 시작 시각
   * @return 나노초 이하를 버린 경과 시간(밀리초)
   */
  public static long elapsedMs(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
  }
}
