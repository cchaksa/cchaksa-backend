// HTTP 요청에서 명시적 예외 캡처와 중복되는 Logback Sentry 이벤트를 제외하는 필터
package com.chukchuk.haksa.global.logging.sentry;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Map;

public class SentryDuplicateEventFilter extends Filter<ILoggingEvent> {

    public static final String MANAGED_REQUEST_MDC_KEY = "sentryManagedRequest";
    private static final String EXCEPTION_HANDLER_LOGGER =
            "com.chukchuk.haksa.global.exception.handler.GlobalExceptionHandler";

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || !"true".equals(mdc.get(MANAGED_REQUEST_MDC_KEY))) {
            return FilterReply.NEUTRAL;
        }

        String logger = event.getLoggerName();
        if (EXCEPTION_HANDLER_LOGGER.equals(logger)
                || (logger != null && logger.startsWith("org.hibernate."))) {
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;
    }
}
