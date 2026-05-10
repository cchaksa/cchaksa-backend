package com.chukchuk.haksa.application.portal;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.CannotCreateTransactionException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.model.SqsException;

final class ScrapeJobOutboxPublisherFailures {

    private ScrapeJobOutboxPublisherFailures() {
    }

    static boolean isPermanentFailure(RuntimeException exception) {
        if (exception instanceof IllegalStateException || exception instanceof IllegalArgumentException) {
            return true;
        }
        if (exception instanceof CannotCreateTransactionException || exception instanceof CannotAcquireLockException) {
            return false;
        }
        if (exception instanceof SqsException sqsException) {
            int statusCode = sqsException.statusCode();
            return statusCode >= 400 && statusCode < 500 && statusCode != 429;
        }
        return !(exception instanceof SdkClientException);
    }
}
