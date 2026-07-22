// 도메인 예외의 Sentry 이슈 제목을 오류 코드 기반으로 정규화하는 콜백
package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.SentryException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SentryEventTitleCallback implements SentryOptions.BeforeSendCallback {

    public static final String ERROR_TITLE_TAG = "error.title";

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        String title = event.getTag(ERROR_TITLE_TAG);
        List<SentryException> exceptions = event.getExceptions();
        if (title == null || title.isBlank() || exceptions == null || exceptions.isEmpty()) {
            return event;
        }

        SentryException outermost = exceptions.get(exceptions.size() - 1);
        outermost.setType(title);
        outermost.setValue(null);
        return event;
    }
}
