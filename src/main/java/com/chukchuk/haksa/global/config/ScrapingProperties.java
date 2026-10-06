package com.chukchuk.haksa.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 스크래핑 작업과 콜백 처리 설정을 제공한다. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "scraping")
public class ScrapingProperties {

  private String mode = "sync";
  private final Job job = new Job();
  private final Callback callback = new Callback();
  private final ResultStore resultStore = new ResultStore();
  private final Scheduler scheduler = new Scheduler();
  private final Publisher publisher = new Publisher();
  private final Stale stale = new Stale();

  /** 스크래핑 작업 발행 설정을 제공한다. */
  @Getter
  @Setter
  public static class Job {
    private String queueUrl;
  }

  /** 스크래핑 콜백 검증 설정을 제공한다. */
  @Getter
  @Setter
  public static class Callback {
    private String hmacSecret = "";
    private long allowedSkewSeconds = 300;
  }

  /** 스크래핑 결과 저장소 설정을 제공한다. */
  @Getter
  @Setter
  public static class ResultStore {
    private String bucket = "";
    private String prefix = "";
    private String region = "ap-northeast-2";
    private long maxPayloadBytes = 2_097_152;
    private long apiCallTimeoutSeconds = 30;
    private long apiCallAttemptTimeoutSeconds = 3;
  }

  /** 스크래핑 예약 작업 설정을 제공한다. */
  @Getter
  @Setter
  public static class Scheduler {
    private boolean enabled = true;
  }

  /** 스크래핑 메시지 발행 설정을 제공한다. */
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
    private long metricsRefreshMs = 60000;
    private final AfterCommit afterCommit = new AfterCommit();

    /** 트랜잭션 커밋 이후 발행 설정을 제공한다. */
    @Getter
    @Setter
    public static class AfterCommit {
      private int executorCorePoolSize = 2;
      private int executorMaxPoolSize = 4;
      private int queueCapacity = 100;
      private int maxAttempts = 5;
      private long initialDelayMs = 250;
      private long maxDelayMs = 5000;
    }
  }

  /** 장기 대기 스크래핑 작업 정리 설정을 제공한다. */
  @Getter
  @Setter
  public static class Stale {
    private boolean enabled = true;
    private long fixedDelayMs = 60000;
    private long timeoutSeconds = 600;
    private int batchSize = 20;
  }
}
