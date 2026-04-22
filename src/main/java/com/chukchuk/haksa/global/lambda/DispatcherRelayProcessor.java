package com.chukchuk.haksa.global.lambda;

import com.chukchuk.haksa.application.portal.ScrapeJobMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;

public final class DispatcherRelayProcessor {

    public static final class RetryableDispatcherException extends RuntimeException {
        public RetryableDispatcherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final ObjectMapper objectMapper;
    private final JobStatusStore jobStatusStore;
    private final WorkerQueueSender workerQueueSender;

    public DispatcherRelayProcessor(ObjectMapper objectMapper, JobStatusStore jobStatusStore, WorkerQueueSender workerQueueSender) {
        this.objectMapper = objectMapper;
        this.jobStatusStore = jobStatusStore;
        this.workerQueueSender = workerQueueSender;
    }

    public void process(String payloadJson, String queueMessageId) throws SQLException {
        ScrapeJobMessage message = parseMessage(payloadJson);
        JobStatusTransitionResult transitionResult = jobStatusStore.markRunningIfQueued(message.job_id());
        if (transitionResult == JobStatusTransitionResult.JOB_NOT_FOUND) {
            return;
        }
        if (transitionResult == JobStatusTransitionResult.SKIPPED) {
            return;
        }

        try {
            workerQueueSender.send(payloadJson, message.message_group_id(), message.message_deduplication_id());
        } catch (RuntimeException exception) {
            jobStatusStore.revertToQueuedIfRunning(message.job_id());
            throw new RetryableDispatcherException("worker queue publish failed for queueMessageId=" + queueMessageId, exception);
        }
    }

    private ScrapeJobMessage parseMessage(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ScrapeJobMessage.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid scrape job message payload", exception);
        }
    }

    public enum JobStatusTransitionResult {
        UPDATED,
        SKIPPED,
        JOB_NOT_FOUND
    }

    public interface JobStatusStore {
        JobStatusTransitionResult markRunningIfQueued(String jobId) throws SQLException;

        void revertToQueuedIfRunning(String jobId) throws SQLException;
    }

    public interface WorkerQueueSender {
        String send(String payloadJson, String messageGroupId, String messageDeduplicationId);
    }

    public static final class JdbcJobStatusStore implements JobStatusStore {

        public JdbcJobStatusStore() {
        }

        @Override
        public JobStatusTransitionResult markRunningIfQueued(String jobId) throws SQLException {
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                String currentStatus = findStatus(connection, jobId);
                if (currentStatus == null) {
                    connection.rollback();
                    return JobStatusTransitionResult.JOB_NOT_FOUND;
                }
                if (!"QUEUED".equalsIgnoreCase(currentStatus)) {
                    connection.rollback();
                    return JobStatusTransitionResult.SKIPPED;
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "update scrape_jobs set status = ?, updated_at = ? where job_id = ? and status = ?"
                )) {
                    statement.setString(1, "RUNNING");
                    statement.setTimestamp(2, Timestamp.from(Instant.now()));
                    statement.setString(3, jobId);
                    statement.setString(4, "QUEUED");
                    int updated = statement.executeUpdate();
                    connection.commit();
                    return updated == 1 ? JobStatusTransitionResult.UPDATED : JobStatusTransitionResult.SKIPPED;
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        }

        @Override
        public void revertToQueuedIfRunning(String jobId) throws SQLException {
            try (Connection connection = openConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "update scrape_jobs set status = ?, updated_at = ? where job_id = ? and status = ?"
                 )) {
                statement.setString(1, "QUEUED");
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.setString(3, jobId);
                statement.setString(4, "RUNNING");
                statement.executeUpdate();
            }
        }

        private static String findStatus(Connection connection, String jobId) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(
                    "select status from scrape_jobs where job_id = ?"
            )) {
                statement.setString(1, jobId);
                try (var resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    return resultSet.getString("status");
                }
            }
        }

        private static Connection openConnection() throws SQLException {
            String profile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "dev")
                    .split(",")[0]
                    .trim()
                    .toLowerCase(Locale.ROOT);
            String prefix = switch (profile) {
                case "prod" -> "PROD";
                case "local", "test" -> "LOCAL";
                default -> "DEV";
            };
            String url = requiredEnv(prefix + "_DB_URL");
            String username = requiredEnv(prefix + "_DB_USERNAME");
            String password = requiredEnv(prefix + "_DB_PASSWORD");
            return DriverManager.getConnection(url, username, password);
        }

        private static String requiredEnv(String key) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required environment variable: " + key);
            }
            return value;
        }
    }

    public static final class SqsWorkerQueueSender implements WorkerQueueSender {

        private final SqsClient sqsClient;
        private final String queueUrl;

        public SqsWorkerQueueSender() {
            this(SqsClient.builder().build(), requiredEnv("SCRAPING_JOB_QUEUE_URL"));
        }

        public SqsWorkerQueueSender(SqsClient sqsClient, String queueUrl) {
            this.sqsClient = sqsClient;
            this.queueUrl = queueUrl;
        }

        @Override
        public String send(String payloadJson, String messageGroupId, String messageDeduplicationId) {
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payloadJson)
                    .build();
            SendMessageResponse response = sqsClient.sendMessage(request);
            return response.messageId();
        }

        private static String requiredEnv(String key) {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing required environment variable: " + key);
            }
            return value;
        }
    }
}
