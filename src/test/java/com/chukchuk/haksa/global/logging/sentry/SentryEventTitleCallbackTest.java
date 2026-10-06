// Sentry 이슈 제목 정규화 콜백을 검증하는 테스트

package com.chukchuk.haksa.global.logging.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.SentryException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SentryEventTitleCallbackTest {

  private final SentryEventTitleCallback callback = new SentryEventTitleCallback();

  @Test
  @DisplayName("error.title 태그가 있으면 최상위 예외 제목을 교체한다")
  void replacesTopLevelExceptionTitleWhenErrorTitleTagExists() {
    SentryException cause = exception("DataAccessException", "constraint violation");
    SentryException outer = exception("CommonException", "기존 메시지");
    SentryEvent event = new SentryEvent();
    event.setExceptions(List.of(cause, outer));
    event.setTag("error.title", "[G02] 졸업요건 데이터 없음");

    SentryEvent result = callback.execute(event, new Hint());

    assertThat(result).isSameAs(event);
    assertThat(cause.getType()).isEqualTo("DataAccessException");
    assertThat(outer.getType()).isEqualTo("[G02] 졸업요건 데이터 없음");
    assertThat(outer.getValue()).isNull();
  }

  @Test
  @DisplayName("error.title 태그가 없으면 예외를 변경하지 않는다")
  void keepsExceptionWhenErrorTitleTagIsMissing() {
    SentryException exception = exception("RuntimeException", "boom");
    SentryEvent event = new SentryEvent();
    event.setExceptions(List.of(exception));

    callback.execute(event, new Hint());

    assertThat(exception.getType()).isEqualTo("RuntimeException");
    assertThat(exception.getValue()).isEqualTo("boom");
  }

  @Test
  @DisplayName("예외 목록이 없으면 이벤트를 그대로 반환한다")
  void keepsEventWhenExceptionListIsEmpty() {
    SentryEvent event = new SentryEvent();
    event.setTag("error.title", "[G02] 졸업요건 데이터 없음");

    assertThat(callback.execute(event, new Hint())).isSameAs(event);
    assertThat(event.getExceptions()).isNull();
  }

  private SentryException exception(String type, String value) {
    SentryException exception = new SentryException();
    exception.setType(type);
    exception.setValue(value);
    return exception;
  }
}
