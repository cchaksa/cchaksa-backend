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
        MDC.put("student_code", "20201234");
        MDC.put("admission_year", "2020");

        Map<String, String> tags = SentryMdcTagBinder.collect();

        assertThat(tags)
                .containsEntry("userId", "user-1")
                .containsEntry("student_code", "20201234")
                .containsEntry("admission_year", "2020")
                .doesNotContainKey("secondary_department_id");
    }
}
