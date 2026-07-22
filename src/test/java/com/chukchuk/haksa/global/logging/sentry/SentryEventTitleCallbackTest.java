// Sentry 이슈 제목 정규화 콜백을 검증하는 테스트
package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.SentryException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SentryEventTitleCallbackTest {

    private final SentryEventTitleCallback callback = new SentryEventTitleCallback();

    @Test
    void errorTitleTag가_있으면_최상위_예외_제목을_교체한다() {
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
    void errorTitleTag가_없으면_예외를_변경하지_않는다() {
        SentryException exception = exception("RuntimeException", "boom");
        SentryEvent event = new SentryEvent();
        event.setExceptions(List.of(exception));

        callback.execute(event, new Hint());

        assertThat(exception.getType()).isEqualTo("RuntimeException");
        assertThat(exception.getValue()).isEqualTo("boom");
    }

    @Test
    void 예외_목록이_없으면_이벤트를_그대로_반환한다() {
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
