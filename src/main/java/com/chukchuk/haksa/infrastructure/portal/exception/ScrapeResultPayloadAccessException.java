package com.chukchuk.haksa.infrastructure.portal.exception;

import lombok.Getter;

@Getter
public class ScrapeResultPayloadAccessException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;

    public ScrapeResultPayloadAccessException(String errorCode, String message, boolean retryable) {
        super(message);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }

    public ScrapeResultPayloadAccessException(String errorCode, String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
    }
}
