package com.chukchuk.haksa.global.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {

    private Sampling sampling = new Sampling();
    private Exclude exclude = new Exclude();

    public Sampling getSampling() { return sampling; }
    public void setSampling(Sampling sampling) { this.sampling = sampling; }

    public Exclude getExclude() { return exclude; }
    public void setExclude(Exclude exclude) { this.exclude = exclude; }

    // ===== 내부 클래스들 =====

    public static class Sampling {
        /** 샘플링 사용 여부 */
        private boolean enabled = false;
        /** 샘플링 비율(0.0 ~ 1.0) */
        private double rate = 1.0;
        /** 샘플링 적용 대상 URI 패턴(접두사 또는 정규식) */
        private List<String> patterns = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }

        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
    }

    public static class Exclude {
        /** 로깅 제외 대상 URI 패턴(접두사 또는 정규식) */
        private List<String> patterns = new ArrayList<>();

        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
    }
}