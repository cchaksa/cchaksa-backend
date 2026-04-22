package com.chukchuk.haksa.infrastructure.scrapejob;

import com.chukchuk.haksa.application.portal.ScrapeJobPublisher;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;

@RequiredArgsConstructor
abstract class AbstractSqsPublisher implements ScrapeJobPublisher {

    protected final ScrapingProperties scrapingProperties;
    private volatile SqsClient sqsClient;

    @Override
    public String publish(String payloadJson) {
        return publish(payloadJson, null, null);
    }

    @Override
    public String publish(String payloadJson, String messageGroupId, String messageDeduplicationId) {
        String queueUrl = resolveQueueUrl();
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("queue url must not be blank");
        }

        SendMessageRequest.Builder builder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payloadJson);

        if (queueUrl.endsWith(".fifo")) {
            if (messageGroupId != null && !messageGroupId.isBlank()) {
                builder.messageGroupId(messageGroupId);
            }
            if (messageDeduplicationId != null && !messageDeduplicationId.isBlank()) {
                builder.messageDeduplicationId(messageDeduplicationId);
            }
        }

        SendMessageResponse response = sqsClient().sendMessage(builder.build());
        return response.messageId();
    }

    protected abstract String resolveQueueUrl();

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
