package com.chukchuk.haksa.domain.scrapejob.model;

public enum ScrapeJobStatus {
    QUEUED,
    RUNNING,
    POST_PROCESSING,
    SUCCEEDED,
    FAILED
}
