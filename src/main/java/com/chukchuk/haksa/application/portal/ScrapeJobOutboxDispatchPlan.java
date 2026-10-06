package com.chukchuk.haksa.application.portal;

import java.util.List;

/**
 * 한 번의 아웃박스 발행 시도에서 처리할 후보와 예약 건수를 전달한다.
 *
 * @param candidates 이번 시도에서 큐에 발행할 아웃박스 목록
 * @param dispatchedCount 발행 대상으로 예약된 아웃박스 수
 */
public record ScrapeJobOutboxDispatchPlan(
    List<ScrapeJobOutboxPublishCandidate> candidates, int dispatchedCount) {

  /**
   * 발행 대상이 없는 빈 계획을 생성한다.
   *
   * @return 후보와 예약 건수가 모두 비어 있는 계획
   */
  public static ScrapeJobOutboxDispatchPlan empty() {
    return new ScrapeJobOutboxDispatchPlan(List.of(), 0);
  }
}
