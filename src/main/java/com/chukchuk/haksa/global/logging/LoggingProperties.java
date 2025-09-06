package com.chukchuk.haksa.global.logging;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {

    private Sampling sampling = new Sampling();
    public Sampling getSampling() { return sampling; }
    public void setSampling(Sampling sampling) { this.sampling = sampling; }

    public static class Sampling {
        /* 샘플링 사용 여부 */
        private boolean enabled = false;
        /* 샘플링 비율(0.0 ~ 1.0) */
        private double rate = 1.0;
        /* 샘플링 적용 대상 URI 패턴(접두사 또는 정규식) */
        // 현재는 과도 트래픽이 발생하는 API 없다고 판단해 공백 유지
        private List<String> patterns = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }
        public List<String> getPatterns() { return patterns; }
        public void setPatterns(List<String> patterns) { this.patterns = patterns; }
    }
}