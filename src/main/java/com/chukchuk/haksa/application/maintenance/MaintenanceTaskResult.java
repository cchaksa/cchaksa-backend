package com.chukchuk.haksa.application.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 유지보수 작업의 성공 여부, 처리 건수 및 원래 예약 시각을 전달한다.
 *
 * @param success 작업이 정상적으로 완료됐는지 여부
 * @param task 실행한 유지보수 작업 이름
 * @param affectedCount 작업으로 변경된 데이터 수
 * @param scheduledAt 요청에 기록된 작업 예약 시각
 */
public record MaintenanceTaskResult(
    boolean success,
    String task,
    @JsonProperty("affected_count") int affectedCount,
    @JsonProperty("scheduled_at") String scheduledAt) {

  /**
   * 성공 결과를 생성한다.
   *
   * @param task 실행한 유지보수 작업 이름
   * @param affectedCount 작업으로 변경된 데이터 수
   * @param scheduledAt 요청에 기록된 작업 예약 시각
   * @return 성공 상태와 처리 건수를 담은 결과
   */
  public static MaintenanceTaskResult success(String task, int affectedCount, String scheduledAt) {
    return new MaintenanceTaskResult(true, task, affectedCount, scheduledAt);
  }
}
