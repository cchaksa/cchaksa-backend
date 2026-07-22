package com.chukchuk.haksa.global.logging.sentry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SentryMdcTagBinderTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void collectReturnsOnlyPresentMdcValues() {
        MDC.put("userId", "user-1");
        MDC.put("jobId", "job-1");
        MDC.put("outboxId", "outbox-1");
        MDC.put("operationType", "LINK");
        MDC.put("workerRequestId", "worker-req-1");
        MDC.put("studentCodeHash", "student-hash");
        MDC.put("admissionYear", "2020");
        MDC.put("departmentId", "38");
        MDC.put("majorType", "SINGLE");
        MDC.put("traceId", "trace-1");

        Map<String, String> tags = SentryMdcTagBinder.collect();

        assertThat(tags)
                .containsEntry("userId", "user-1")
                .containsEntry("jobId", "job-1")
                .containsEntry("outboxId", "outbox-1")
                .containsEntry("operationType", "LINK")
                .containsEntry("workerRequestId", "worker-req-1")
                .containsEntry("studentCodeHash", "student-hash")
                .containsEntry("admissionYear", "2020")
                .containsEntry("departmentId", "38")
                .containsEntry("majorType", "SINGLE")
                .containsEntry("traceId", "trace-1")
                .doesNotContainKey("student_code")
                .doesNotContainKey("secondaryDepartmentId");
    }
}
