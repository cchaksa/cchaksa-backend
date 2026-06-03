// Sentry 요청 컨텍스트 바인딩 동작을 검증하는 테스트
package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.ISentryLifecycleToken;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class SentryMdcContextTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void runBindsValuesDuringActionAndClearsAfterward() {
        SentryMdcContext.Context context = new SentryMdcContext.Context(
                "user-1",
                "job-1",
                "outbox-1",
                "LINK",
                "worker-req-1"
        );

        SentryMdcContext.run(context, () -> {
            assertThat(MDC.get("userId")).isEqualTo("user-1");
            assertThat(MDC.get("jobId")).isEqualTo("job-1");
            assertThat(MDC.get("outboxId")).isEqualTo("outbox-1");
            assertThat(MDC.get("operationType")).isEqualTo("LINK");
            assertThat(MDC.get("workerRequestId")).isEqualTo("worker-req-1");
        });

        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("jobId")).isNull();
        assertThat(MDC.get("outboxId")).isNull();
        assertThat(MDC.get("operationType")).isNull();
        assertThat(MDC.get("workerRequestId")).isNull();
    }

    @Test
    void openFromRequestBindsStoredContextAndClearsAfterward() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        SentryMdcContext.bindToRequest(request, new SentryMdcContext.Context(
                "user-1",
                "job-1",
                "outbox-1",
                "REFRESH",
                "worker-req-1"
        ));

        try (SentryMdcContext.MdcScope ignored = SentryMdcContext.openFromRequest(request)) {
            assertThat(MDC.get("userId")).isEqualTo("user-1");
            assertThat(MDC.get("jobId")).isEqualTo("job-1");
            assertThat(MDC.get("outboxId")).isEqualTo("outbox-1");
            assertThat(MDC.get("operationType")).isEqualTo("REFRESH");
            assertThat(MDC.get("workerRequestId")).isEqualTo("worker-req-1");
        }

        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("jobId")).isNull();
    }

    @Test
    void openWithUserPushesSentryScopeAndClosesIt() {
        ISentryLifecycleToken token = Mockito.mock(ISentryLifecycleToken.class);

        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class)) {
            sentry.when(Sentry::pushScope).thenReturn(token);

            try (SentryMdcContext.MdcScope ignored = SentryMdcContext.open(new SentryMdcContext.Context(
                    "user-1",
                    "job-1",
                    null,
                    "LINK",
                    null
            ))) {
                assertThat(MDC.get("userId")).isEqualTo("user-1");
            }

            sentry.verify(Sentry::pushScope, times(1));
            sentry.verify(() -> Sentry.setUser(any(User.class)), times(1));
            Mockito.verify(token).close();
            sentry.verify(() -> Sentry.setUser(null), never());
        }
    }

    @Test
    void openWithoutUserDoesNotClearSentryUserOnClose() {
        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class)) {
            try (SentryMdcContext.MdcScope ignored = SentryMdcContext.open(new SentryMdcContext.Context(
                    null,
                    "job-1",
                    null,
                    "LINK",
                    null
            ))) {
                assertThat(MDC.get("jobId")).isEqualTo("job-1");
            }

            sentry.verify(() -> Sentry.setUser(any(User.class)), never());
            sentry.verify(() -> Sentry.setUser(null), never());
        }
    }

    @Test
    void fromConvertsNullableUserAndEnumValues() {
        UUID userId = UUID.randomUUID();

        SentryMdcContext.Context populated = SentryMdcContext.from(
                userId,
                "job-1",
                "outbox-1",
                TestOperation.REFRESH,
                "worker-req-1"
        );
        SentryMdcContext.Context empty = SentryMdcContext.from(null, "job-2", "outbox-2", null, null);

        assertThat(populated.userId()).isEqualTo(userId.toString());
        assertThat(populated.operationType()).isEqualTo("REFRESH");
        assertThat(empty.userId()).isNull();
        assertThat(empty.operationType()).isNull();
    }

    private enum TestOperation {
        REFRESH
    }
}
