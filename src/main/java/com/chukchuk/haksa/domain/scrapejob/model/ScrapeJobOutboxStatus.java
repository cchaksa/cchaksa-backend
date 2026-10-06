package com.chukchuk.haksa.domain.scrapejob.model;

/** 업무 처리에서 사용할 스크래핑 작업 아웃박스 상태을 정의한다. */
public enum ScrapeJobOutboxStatus {
  PENDING,
  SENT,
  RETRYABLE_FAILED,
  DEAD
}
