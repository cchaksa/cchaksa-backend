package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.IScope;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;

/** Utility that promotes selected MDC keys to Sentry tags. */
public final class SentryMdcTagBinder {

  private static final List<String> TAG_KEYS =
      List.of(
          "userId",
          "jobId",
          "outboxId",
          "operationType",
          "workerRequestId",
          "studentCodeHash",
          "admissionYear",
          "departmentId",
          "secondaryDepartmentId",
          "majorType",
          "traceId");

  private SentryMdcTagBinder() {}

  /**
   * 설정된 태그 키에 대응하는 현재 MDC 값을 수집한다.
   *
   * @return MDC에 존재하는 키와 값의 순서 보존 map
   */
  public static Map<String, String> collect() {
    Map<String, String> tags = new LinkedHashMap<>();
    for (String key : TAG_KEYS) {
      String value = MDC.get(key);
      if (value != null) {
        tags.put(key, value);
      }
    }
    return tags;
  }

  /**
   * 현재 MDC에서 수집한 태그를 Sentry scope에 적용한다.
   *
   * @param scope MDC 태그를 추가할 Sentry scope
   */
  public static void bind(IScope scope) {
    collect().forEach(scope::setTag);
  }

  /**
   * MDC에서 Sentry 태그로 승격하는 키 목록을 반환한다.
   *
   * @return 변경할 수 없는 태그 키 목록
   */
  public static List<String> tagKeys() {
    return TAG_KEYS;
  }
}
