package com.chukchuk.haksa.global.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.CommonException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldSkipSentryCaptureForScrapeJobClientErrors() {
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_FAILED_RESULT))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_NOT_COMPLETED))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_NOT_FOUND))).isFalse();
    }

    @Test
    void shouldSkipSentryCaptureForExpectedPortalClientErrors() {
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.PORTAL_LOGIN_FAILED))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.PORTAL_ACCOUNT_LOCKED))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.INVALID_CALLBACK_SIGNATURE))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_INVALID_CALLBACK_REQUEST))).isFalse();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_INVALID_S3_KEY))).isFalse();
    }

    @Test
    void shouldSkipSentryCaptureForExpectedLectureEvaluationErrors() {
        assertThat(handler.shouldReportBaseException(
                new CommonException(ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED))).isFalse();
        assertThat(handler.shouldReportBaseException(
                new CommonException(ErrorCode.LECTURE_EVALUATION_COURSE_MISMATCH))).isFalse();
    }

    @Test
    void shouldSkipMissingUserOnlyOnRefreshEndpoint() {
        EntityNotFoundException exception = new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);

        assertThat(handler.shouldReportEntityNotFound(exception, request("/api/auth/refresh"))).isFalse();
        assertThat(handler.shouldReportEntityNotFound(exception, request("/api/users/me"))).isTrue();
    }

    @Test
    void shouldReportSystemPortalExceptions() {
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_JOB_ENQUEUE_FAILED))).isTrue();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_RESULT_S3_FAILED))).isTrue();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_RESULT_SCHEMA_INVALID))).isTrue();
        assertThat(handler.shouldReportBaseException(new CommonException(ErrorCode.SCRAPE_RESULT_POST_PROCESSING_FAILED))).isTrue();
    }

    @Test
    void shouldReportNonCommonBaseExceptions() {
        assertThat(handler.shouldReportBaseException(new EntityNotFoundException(ErrorCode.USER_NOT_FOUND))).isTrue();
    }

    private HttpServletRequest request(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }
}
