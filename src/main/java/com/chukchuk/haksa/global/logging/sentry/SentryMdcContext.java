// Sentry 이벤트 검색을 위한 MDC 컨텍스트 스코프 헬퍼
package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SentryMdcContext {

    private static final String USER_ID = "userId";
    private static final String JOB_ID = "jobId";
    private static final String OUTBOX_ID = "outboxId";
    private static final String OPERATION_TYPE = "operationType";
    private static final String WORKER_REQUEST_ID = "workerRequestId";
    private static final String ATTR_PREFIX = SentryMdcContext.class.getName() + ".";

    private SentryMdcContext() {
    }

    public static void run(Context context, Runnable action) {
        try (MdcScope ignored = open(context)) {
            action.run();
        }
    }

    public static <T> T supply(Context context, Supplier<T> supplier) {
        try (MdcScope ignored = open(context)) {
            return supplier.get();
        }
    }

    public static MdcScope open(Context context) {
        return new MdcScope(context);
    }

    public static void bindToCurrentRequest(Context context) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            bindToRequest(servletAttributes.getRequest(), context);
        }
    }

    public static void bindToRequest(HttpServletRequest request, Context context) {
        if (request == null || context == null) {
            return;
        }
        setAttribute(request, USER_ID, context.userId());
        setAttribute(request, JOB_ID, context.jobId());
        setAttribute(request, OUTBOX_ID, context.outboxId());
        setAttribute(request, OPERATION_TYPE, context.operationType());
        setAttribute(request, WORKER_REQUEST_ID, context.workerRequestId());
    }

    public static MdcScope openFromRequest(HttpServletRequest request) {
        Context context = contextFromRequest(request);
        if (context == null) {
            return MdcScope.noop();
        }
        return open(context);
    }

    public record Context(
            String userId,
            String jobId,
            String outboxId,
            String operationType,
            String workerRequestId
    ) {
    }

    public static final class MdcScope implements AutoCloseable {
        private final Map<String, String> previousValues = new LinkedHashMap<>();
        private final boolean noop;

        private MdcScope(Context context) {
            this.noop = false;
            put(USER_ID, context.userId());
            put(JOB_ID, context.jobId());
            put(OUTBOX_ID, context.outboxId());
            put(OPERATION_TYPE, context.operationType());
            put(WORKER_REQUEST_ID, context.workerRequestId());

            if (hasText(context.userId())) {
                User sentryUser = new User();
                sentryUser.setId(context.userId());
                Sentry.setUser(sentryUser);
            }
        }

        private MdcScope() {
            this.noop = true;
        }

        private static MdcScope noop() {
            return new MdcScope();
        }

        private void put(String key, String value) {
            previousValues.put(key, MDC.get(key));
            if (hasText(value)) {
                MDC.put(key, value);
            } else {
                MDC.remove(key);
            }
        }

        @Override
        public void close() {
            if (noop) {
                return;
            }
            previousValues.forEach((key, value) -> {
                if (value == null) {
                    MDC.remove(key);
                } else {
                    MDC.put(key, value);
                }
            });
            Sentry.setUser(null);
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }

    private static Context contextFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userId = attribute(request, USER_ID);
        String jobId = attribute(request, JOB_ID);
        String outboxId = attribute(request, OUTBOX_ID);
        String operationType = attribute(request, OPERATION_TYPE);
        String workerRequestId = attribute(request, WORKER_REQUEST_ID);
        if (!hasText(userId) && !hasText(jobId) && !hasText(outboxId) && !hasText(operationType) && !hasText(workerRequestId)) {
            return null;
        }
        return new Context(userId, jobId, outboxId, operationType, workerRequestId);
    }

    private static void setAttribute(HttpServletRequest request, String key, String value) {
        if (hasText(value)) {
            request.setAttribute(ATTR_PREFIX + key, value);
        }
    }

    private static String attribute(HttpServletRequest request, String key) {
        Object value = request.getAttribute(ATTR_PREFIX + key);
        return value instanceof String stringValue ? stringValue : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
