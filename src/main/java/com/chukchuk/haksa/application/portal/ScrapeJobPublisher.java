package com.chukchuk.haksa.application.portal;

public interface ScrapeJobPublisher {

    String publish(String payloadJson);

    default String publish(String payloadJson, String messageGroupId, String messageDeduplicationId) {
        return publish(payloadJson);
    }
}
