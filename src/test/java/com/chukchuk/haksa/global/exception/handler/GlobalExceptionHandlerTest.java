package com.chukchuk.haksa.global.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldSkipSentryCaptureForScrapeJobClientErrors() {
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_FAILED_RESULT))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_NOT_COMPLETED))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_NOT_FOUND))).isFalse();
    }

    @Test
    void shouldReportOtherCommonExceptions() {
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.INVALID_ARGUMENT))).isTrue();
    }

    @Test
    void shouldReportNonCommonBaseExceptions() {
        assertThat(handler.shouldReportBaseException(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND))).isTrue();
    }
}
