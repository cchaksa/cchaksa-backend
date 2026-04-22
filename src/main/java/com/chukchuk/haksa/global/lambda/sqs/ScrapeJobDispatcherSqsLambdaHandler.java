package com.chukchuk.haksa.global.lambda.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.chukchuk.haksa.global.lambda.DispatcherRelayProcessor;
import com.chukchuk.haksa.global.lambda.DispatcherFailureReporter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ScrapeJobDispatcherSqsLambdaHandler implements RequestStreamHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private final DispatcherRelayProcessor relayProcessor;
    private final DispatcherFailureReporter failureReporter;

    public ScrapeJobDispatcherSqsLambdaHandler() {
        this(new DispatcherRelayProcessor(
                OBJECT_MAPPER,
                new DispatcherRelayProcessor.JdbcJobStatusStore(),
                new DispatcherRelayProcessor.SqsWorkerQueueSender()
        ), new DispatcherFailureReporter());
    }

    public ScrapeJobDispatcherSqsLambdaHandler(DispatcherRelayProcessor relayProcessor, DispatcherFailureReporter failureReporter) {
        this.relayProcessor = relayProcessor;
        this.failureReporter = failureReporter;
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        JsonNode event = OBJECT_MAPPER.readTree(input);
        JsonNode records = event.path("Records");
        if (!records.isArray()) {
            throw new IllegalArgumentException("SQS event must contain Records array");
        }

        for (JsonNode record : records) {
            String messageId = record.path("messageId").asText("");
            String payloadJson = record.path("body").asText("");
            log.info("[BIZ] scrape.job.request_dispatch.receive awsRequestId={} queueMessageId={}",
                    context.getAwsRequestId(), messageId);
            try {
                relayProcessor.process(payloadJson, messageId);
            } catch (IllegalArgumentException exception) {
                failureReporter.reportTerminal("DISPATCHER_INVALID_MESSAGE", messageId, null, exception);
                return;
            } catch (DispatcherRelayProcessor.RetryableDispatcherException exception) {
                failureReporter.reportRetryable("DISPATCHER_PUBLISH_FAILED", messageId, null, exception);
                throw new IOException(exception.getMessage(), exception);
            } catch (java.sql.SQLException exception) {
                failureReporter.reportRetryable("DISPATCHER_DB_FAILED", messageId, null, exception);
                throw new IOException("Failed to update scrape job status", exception);
            }
        }

        output.write("{}".getBytes(StandardCharsets.UTF_8));
    }
}
