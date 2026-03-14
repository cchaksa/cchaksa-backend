package com.chukchuk.haksa.domain.scrapejob.model;

public enum ScrapeJobOutboxStatus {
    PENDING,
    SENT,
    RETRYABLE_FAILED,
    DEAD
}
