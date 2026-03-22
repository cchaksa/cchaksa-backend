package com.chukchuk.haksa.global.logging;

import com.chukchuk.haksa.global.logging.sentry.SentryMdcTagBinder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackConfigTest {

    private static final Path LOGBACK_XML = Path.of("src/main/resources/logback-spring.xml");

    @Test
    void sentryAppendersExposeGraduationMdcTags() throws IOException {
        String xml = Files.readString(LOGBACK_XML);

        assertThat(xml)
                .contains("<appender name=\"SENTRY_DEV\" class=\"io.sentry.logback.SentryAppender\">")
                .contains("<appender name=\"SENTRY_PROD\" class=\"io.sentry.logback.SentryAppender\">");

        for (String tag : SentryMdcTagBinder.tagKeys()) {
            assertThat(xml).contains("<tag>" + tag + "</tag>");
        }
    }
}
