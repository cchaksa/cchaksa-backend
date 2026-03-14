package com.chukchuk.haksa.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scraping")
public class ScrapingProperties {

    private String mode = "sync";
    private final Job job = new Job();
    private final Callback callback = new Callback();
    private final Publisher publisher = new Publisher();
    private final Stale stale = new Stale();

    @Getter
    @Setter
    public static class Job {
        private String queueUrl;
    }

    @Getter
    @Setter
    public static class Callback {
        private String hmacSecret = "";
        private long allowedSkewSeconds = 300;
    }

    @Getter
    @Setter
    public static class Publisher {
        private boolean enabled = true;
        private long fixedDelayMs = 10000;
        private int batchSize = 20;
        private int maxAttempts = 5;
        private long initialBackoffSeconds = 5;
        private long maxBackoffSeconds = 300;
        private long apiCallTimeoutSeconds = 10;
        private long apiCallAttemptTimeoutSeconds = 5;
    }

    @Getter
    @Setter
    public static class Stale {
        private boolean enabled = true;
        private long fixedDelayMs = 60000;
        private long timeoutSeconds = 600;
        private int batchSize = 20;
    }
}
