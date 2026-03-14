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
}
