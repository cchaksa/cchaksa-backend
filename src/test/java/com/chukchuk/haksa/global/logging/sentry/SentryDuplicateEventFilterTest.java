// 동일 요청에서 중복 생성되는 Sentry 로그 이벤트 필터를 검증하는 테스트
package com.chukchuk.haksa.global.logging.sentry;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SentryDuplicateEventFilterTest {

    private final SentryDuplicateEventFilter filter = new SentryDuplicateEventFilter();

    @Test
    void 관리_요청의_예외_핸들러와_Hibernate_로그는_제외한다() {
        assertThat(filter.decide(event(
                "com.chukchuk.haksa.global.exception.handler.GlobalExceptionHandler", true)))
                .isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event("org.hibernate.engine.jdbc.spi.SqlExceptionHelper", true)))
                .isEqualTo(FilterReply.DENY);
    }

    @Test
    void 다른_애플리케이션_로그와_백그라운드_로그는_유지한다() {
        assertThat(filter.decide(event("com.chukchuk.haksa.application.Worker", true)))
                .isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event("org.hibernate.SQL", false)))
                .isEqualTo(FilterReply.NEUTRAL);
    }

    private ILoggingEvent event(String loggerName, boolean managedRequest) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn(loggerName);
        when(event.getMDCPropertyMap()).thenReturn(managedRequest
                ? Map.of(SentryDuplicateEventFilter.MANAGED_REQUEST_MDC_KEY, "true")
                : Map.of());
        return event;
    }
}
