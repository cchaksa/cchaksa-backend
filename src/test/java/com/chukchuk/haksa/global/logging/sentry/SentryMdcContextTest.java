// Sentry 요청 컨텍스트 바인딩 동작을 검증하는 테스트
package com.chukchuk.haksa.global.logging.sentry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

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
}
