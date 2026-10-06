package com.chukchuk.haksa.application.maintenance;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/** 유지보수 task type에서 사용할 값을 정의한다. */
public enum MaintenanceTaskType {
  SCRAPE_JOB_RECONCILE_STALE,
  REFRESH_TOKEN_CLEANUP;

  private static final Map<String, MaintenanceTaskType> LOOKUP =
      Arrays.stream(values())
          .collect(Collectors.toUnmodifiableMap(Enum::name, taskType -> taskType));

  /**
   * 외부 작업 이름에 대응하는 유지보수 작업 유형을 반환한다.
   *
   * @param value 대소문자를 포함해 enum 이름과 정확히 일치해야 하는 작업 이름
   * @return 작업 이름에 대응하는 유지보수 작업 유형
   * @throws IllegalArgumentException 등록되지 않은 작업 이름인 경우
   */
  public static MaintenanceTaskType from(String value) {
    MaintenanceTaskType taskType = LOOKUP.get(value);
    if (taskType == null) {
      throw new IllegalArgumentException("Unknown maintenance task: " + value);
    }
    return taskType;
  }
}
