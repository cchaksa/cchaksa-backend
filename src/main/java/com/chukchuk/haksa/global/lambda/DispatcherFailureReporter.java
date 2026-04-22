package com.chukchuk.haksa.global.lambda;

import com.chukchuk.haksa.global.logging.sentry.SentryMdcTagBinder;
import io.sentry.Sentry;
import io.sentry.SentryLevel;

import java.util.List;

public final class DispatcherFailureReporter {

    public void reportTerminal(String errorCode, String queueMessageId, String jobId, Throwable exception) {
        Sentry.withScope(scope -> {
            scope.setTag("error.type", "DISPATCHER_TERMINAL");
            scope.setTag("error.code", errorCode);
            scope.setTag("queue.message_id", queueMessageId);
            if (jobId != null && !jobId.isBlank()) {
                scope.setTag("job.id", jobId);
            }
            scope.setLevel(SentryLevel.WARNING);
            scope.setFingerprint(List.of("DISPATCHER_TERMINAL", errorCode));
            SentryMdcTagBinder.bind(scope);
            Sentry.captureException(exception);
        });
    }

    public void reportRetryable(String errorCode, String queueMessageId, String jobId, Throwable exception) {
        Sentry.withScope(scope -> {
            scope.setTag("error.type", "DISPATCHER_RETRYABLE");
            scope.setTag("error.code", errorCode);
            scope.setTag("queue.message_id", queueMessageId);
            if (jobId != null && !jobId.isBlank()) {
                scope.setTag("job.id", jobId);
            }
            scope.setLevel(SentryLevel.ERROR);
            scope.setFingerprint(List.of("DISPATCHER_RETRYABLE", errorCode));
            SentryMdcTagBinder.bind(scope);
            Sentry.captureException(exception);
        });
    }
}
