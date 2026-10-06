package com.chukchuk.haksa.domain.scrapejob.model;

/** 업무 처리에서 사용할 스크래핑 작업 상태을 정의한다. */
public enum ScrapeJobStatus {
  QUEUED,
  RUNNING,
  POST_PROCESSING,
  SUCCEEDED,
  FAILED
}
