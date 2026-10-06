package com.chukchuk.haksa.infrastructure.scrapejob;

import com.chukchuk.haksa.global.config.ScrapingProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/** 스크래핑 작업 메시지를 SQS에 발행한다. */
@Component
@RequiredArgsConstructor
public class SqsScrapeJobPublisher {

  private final ScrapingProperties scrapingProperties;
  private volatile SqsClient sqsClient;

  /**
   * 스크래핑 작업 메시지를 큐에 발행하고 메시지 식별자를 반환한다.
   *
   * @param payloadJson 워커가 처리할 스크래핑 작업 JSON
   * @return SQS가 발급한 메시지 식별자
   * @throws IllegalStateException 스크래핑 작업 queue URL이 설정되지 않은 경우
   */
  public String publish(String payloadJson) {
    String queueUrl = scrapingProperties.getJob().getQueueUrl();
    if (queueUrl == null || queueUrl.isBlank()) {
      throw new IllegalStateException("scraping.job.queue-url must not be blank");
    }

    SendMessageResponse response =
        sqsClient()
            .sendMessage(
                SendMessageRequest.builder().queueUrl(queueUrl).messageBody(payloadJson).build());
    return response.messageId();
  }

  private SqsClient sqsClient() {
    if (sqsClient == null) {
      synchronized (this) {
        if (sqsClient == null) {
          sqsClient =
              SqsClient.builder()
                  .overrideConfiguration(
                      ClientOverrideConfiguration.builder()
                          .apiCallTimeout(
                              Duration.ofSeconds(
                                  scrapingProperties.getPublisher().getApiCallTimeoutSeconds()))
                          .apiCallAttemptTimeout(
                              Duration.ofSeconds(
                                  scrapingProperties
                                      .getPublisher()
                                      .getApiCallAttemptTimeoutSeconds()))
                          .retryStrategy(RetryMode.STANDARD)
                          .build())
                  .build();
        }
      }
    }
    return sqsClient;
  }
}
