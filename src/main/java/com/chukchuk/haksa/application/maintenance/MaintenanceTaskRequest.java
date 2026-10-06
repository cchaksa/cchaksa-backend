package com.chukchuk.haksa.application.maintenance;

/**
 * EventBridge가 요청한 유지보수 작업 이름과 예약 시각을 전달한다.
 *
 * @param source 작업을 요청한 실행 주체
 * @param task 실행할 유지보수 작업 이름
 * @param scheduledAt 예약 시각
 */
public record MaintenanceTaskRequest(String source, String task, String scheduledAt) {}
