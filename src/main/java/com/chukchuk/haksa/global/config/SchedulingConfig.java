package com.chukchuk.haksa.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/** 스크래핑 예약 실행이 활성화된 환경에 작업 스케줄러를 구성한다. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "scraping.scheduler", name = "enabled", havingValue = "true")
public class SchedulingConfig {

  /**
   * 예약 작업에 사용할 스레드 풀 스케줄러를 생성한다.
   *
   * @return 두 개의 작업 스레드와 종료 대기 정책이 적용된 스케줄러
   */
  @Bean
  public ThreadPoolTaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(2);
    taskScheduler.setThreadNamePrefix("scheduler-");
    taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
    taskScheduler.setAwaitTerminationSeconds(30);
    return taskScheduler;
  }
}
