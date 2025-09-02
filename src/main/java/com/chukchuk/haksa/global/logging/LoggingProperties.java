package com.chukchuk.haksa.global.logging;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {

    private Sampling sampling = new Sampling();

    public Sampling getSampling() { return sampling; }
    public void setSampling(Sampling sampling) { this.sampling = sampling; }

    public static class Sampling {
        /** 고트래픽 엔드포인트 샘플링 비율 (예: /api/v1/list) */
        private double heavyEndpoints = 0.2; // dev 기본값

        public double getHeavyEndpoints() { return heavyEndpoints; }
        public void setHeavyEndpoints(double heavyEndpoints) { this.heavyEndpoints = heavyEndpoints; }
    }
}