package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.IScope;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility that promotes selected MDC keys to Sentry tags.
 */
public final class SentryMdcTagBinder {

    private static final List<String> TAG_KEYS = List.of(
            "userId",
            "student_code",
            "admission_year",
            "primary_department_id",
            "secondary_department_id",
            "major_type"
    );

    private SentryMdcTagBinder() {
    }

    /**
     * Collects MDC values for the configured tag keys.
     */
    public static Map<String, String> collect() {
        Map<String, String> tags = new LinkedHashMap<>();
        for (String key : TAG_KEYS) {
            String value = MDC.get(key);
            if (value != null) {
                tags.put(key, value);
            }
        }
        return tags;
    }

    /**
     * Applies MDC tags to the provided Sentry scope.
     */
    public static void bind(IScope scope) {
        collect().forEach(scope::setTag);
    }

    /**
     * Exposes tag keys for verification (e.g., configuration tests).
     */
    public static List<String> tagKeys() {
        return TAG_KEYS;
    }
}
