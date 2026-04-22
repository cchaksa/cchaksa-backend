package com.chukchuk.haksa.global.lambda.sqs;

import com.chukchuk.haksa.global.lambda.DispatcherRelayProcessor;
import com.chukchuk.haksa.global.lambda.DispatcherFailureReporter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScrapeJobDispatcherSqsLambdaHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void handlesSqsRecordsAndWritesEmptyJsonObject() throws Exception {
        DispatcherRelayProcessor relayProcessor = mock(DispatcherRelayProcessor.class);
        DispatcherFailureReporter failureReporter = mock(DispatcherFailureReporter.class);
        ScrapeJobDispatcherSqsLambdaHandler handler = new ScrapeJobDispatcherSqsLambdaHandler(relayProcessor, failureReporter);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(input(eventJson()), output, new DummyContext());

        verify(relayProcessor).process("{\"job_id\":\"job-1\"}", "queue-message-1");
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo("{}");
    }

    @Test
    void wrapsSqlExceptionAsIoException() throws Exception {
        DispatcherRelayProcessor relayProcessor = mock(DispatcherRelayProcessor.class);
        doThrow(new SQLException("db down")).when(relayProcessor).process(anyString(), anyString());
        DispatcherFailureReporter failureReporter = mock(DispatcherFailureReporter.class);
        ScrapeJobDispatcherSqsLambdaHandler handler = new ScrapeJobDispatcherSqsLambdaHandler(relayProcessor, failureReporter);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                handler.handleRequest(input(eventJson()), output, new DummyContext()))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Failed to update scrape job status");
    }

    private static String eventJson() throws Exception {
        ObjectNode event = OBJECT_MAPPER.createObjectNode();
        ArrayNode records = event.putArray("Records");
        ObjectNode record = records.addObject();
        record.put("messageId", "queue-message-1");
        record.put("body", "{\"job_id\":\"job-1\"}");
        return OBJECT_MAPPER.writeValueAsString(event);
    }

    private static InputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static class DummyContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "dummy-request-id";
        }

        @Override
        public String getLogGroupName() {
            return "dummy-log-group";
        }

        @Override
        public String getLogStreamName() {
            return "dummy-log-stream";
        }

        @Override
        public String getFunctionName() {
            return "dummy-function";
        }

        @Override
        public String getFunctionVersion() {
            return "1";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn:aws:lambda:local:0:function:dummy";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 30_000;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 512;
        }

        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {
                }

                @Override
                public void log(byte[] message) {
                }
            };
        }
    }
}
