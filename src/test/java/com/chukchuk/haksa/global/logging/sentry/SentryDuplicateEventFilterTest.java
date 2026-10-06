// 동일 요청에서 중복 생성되는 Sentry 로그 이벤트 필터를 검증하는 테스트

package com.chukchuk.haksa.global.logging.sentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SentryDuplicateEventFilterTest {

  private final SentryDuplicateEventFilter filter = new SentryDuplicateEventFilter();

  @Test
  @DisplayName("관리 요청의 예외 핸들러와 Hibernate 로그는 제외한다")
  void dropsHandledAdminAndHibernateEvents() {
    assertThat(
            filter.decide(
                event("com.chukchuk.haksa.global.exception.handler.GlobalExceptionHandler", true)))
        .isEqualTo(FilterReply.DENY);
    assertThat(filter.decide(event("org.hibernate.engine.jdbc.spi.SqlExceptionHelper", true)))
        .isEqualTo(FilterReply.DENY);
  }

  @Test
  @DisplayName("다른 애플리케이션 로그와 백그라운드 로그는 유지한다")
  void keepsOtherApplicationAndBackgroundEvents() {
    assertThat(filter.decide(event("com.chukchuk.haksa.application.Worker", true)))
        .isEqualTo(FilterReply.NEUTRAL);
    assertThat(filter.decide(event("org.hibernate.SQL", false))).isEqualTo(FilterReply.NEUTRAL);
  }

  private ILoggingEvent event(String loggerName, boolean managedRequest) {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getLoggerName()).thenReturn(loggerName);
    when(event.getMDCPropertyMap())
        .thenReturn(
            managedRequest
                ? Map.of(SentryDuplicateEventFilter.MANAGED_REQUEST_MDC_KEY, "true")
                : Map.of());
    return event;
  }
}
