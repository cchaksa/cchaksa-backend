package com.chukchuk.haksa.infrastructure.scrapejob;

import com.chukchuk.haksa.application.portal.ScrapeJobMessage;
import com.chukchuk.haksa.application.portal.ScrapeJobPublisher;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SqsScrapeJobPublisher implements ScrapeJobPublisher {

    private final ScrapingProperties scrapingProperties;
    private volatile SqsClient sqsClient;

    @Override
    public String publish(String payloadJson) {
        String queueUrl = scrapingProperties.getJob().getQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("scraping.job.queue-url must not be blank");
        }

        SendMessageResponse response = sqsClient().sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payloadJson)
                .build());
        return response.messageId();
    }

    private SqsClient sqsClient() {
        if (sqsClient == null) {
            synchronized (this) {
                if (sqsClient == null) {
                    sqsClient = SqsClient.builder()
                            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                    .apiCallTimeout(Duration.ofSeconds(scrapingProperties.getPublisher().getApiCallTimeoutSeconds()))
                                    .apiCallAttemptTimeout(Duration.ofSeconds(scrapingProperties.getPublisher().getApiCallAttemptTimeoutSeconds()))
                                    .retryStrategy(RetryMode.STANDARD)
                                    .build())
                            .build();
                }
            }
        }
        return sqsClient;
    }
}
