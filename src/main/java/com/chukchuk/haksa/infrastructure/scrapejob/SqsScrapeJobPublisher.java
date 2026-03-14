package com.chukchuk.haksa.infrastructure.scrapejob;

import com.chukchuk.haksa.application.portal.ScrapeJobMessage;
import com.chukchuk.haksa.application.portal.ScrapeJobPublisher;
import com.chukchuk.haksa.global.config.ScrapingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Component
@RequiredArgsConstructor
public class SqsScrapeJobPublisher implements ScrapeJobPublisher {

    private final ScrapingProperties scrapingProperties;
    private final ObjectMapper objectMapper;
    private volatile SqsClient sqsClient;

    @Override
    public String publish(ScrapeJobMessage message) {
        try {
            String queueUrl = scrapingProperties.getJob().getQueueUrl();
            if (queueUrl == null || queueUrl.isBlank()) {
                throw new IllegalStateException("scraping.job.queue-url must not be blank");
            }
            SendMessageResponse response = sqsClient().sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .build());
            return response.messageId();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize scrape job message", e);
        }
    }

    private SqsClient sqsClient() {
        if (sqsClient == null) {
            synchronized (this) {
                if (sqsClient == null) {
                    sqsClient = SqsClient.builder().build();
                }
            }
        }
        return sqsClient;
    }
}
