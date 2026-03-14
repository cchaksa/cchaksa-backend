package com.chukchuk.haksa.global.lambda;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2HttpContext;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StreamLambdaHandlerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        System.setProperty("LOG_PATH", "build/tmp/lambda-test-logs");
        System.setProperty("LOG_FILE_NAME", "lambda-test");
        System.setProperty("SPRING_PROFILES_ACTIVE", "test");
        System.setProperty("spring.profiles.active", "test");
        System.setProperty("JWT_SECRET", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        System.setProperty("JWT_ACCESS_EXPIRATION", "3600");
        System.setProperty("JWT_REFRESH_EXPIRATION", "86400");
        System.setProperty("APP_KEY", "test-app-key");
        System.setProperty("APP_NATIVE_KEY", "test-native-key");
        System.setProperty("CRAWLER_BASE_URL", "https://example.com");
        System.setProperty("DEV_DB_URL", "jdbc:h2:mem:lambda-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        System.setProperty("DEV_DB_USERNAME", "sa");
        System.setProperty("DEV_DB_PASSWORD", "");
        System.setProperty("LOCAL_DB_URL", "jdbc:h2:mem:lambda-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        System.setProperty("LOCAL_DB_USERNAME", "sa");
        System.setProperty("LOCAL_DB_PASSWORD", "");
        System.setProperty("DEV_SENTRY_DSN", "");
    }

    @Test
    void registersDispatcherServlet() throws Exception {
        StreamLambdaHandler handler = new StreamLambdaHandler();
        SpringBootLambdaContainerHandler<?, ?> containerHandler = extractContainerHandler();

        Map<String, ? extends ServletRegistration> registrations =
                containerHandler.getServletContext().getServletRegistrations();

        assertThat(handler).isNotNull();
        assertThat(registrations).containsKey("dispatcherServlet");
        assertThat(registrations.get("dispatcherServlet").getMappings()).contains("/");
    }

    @Test
    void healthCheckRespondsWithPlainTextBody() throws Exception {
        AwsProxyResponse response = invoke("/health", "GET", null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("ok");
    }

    @Test
    void apiDocsRespondWithJsonBody() throws Exception {
        AwsProxyResponse response = invoke("/v3/api-docs", "GET", null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getBody()).isNotBlank();
        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(jsonNode.has("openapi")).isTrue();
    }

    @Test
    void missingPathRespondsWithErrorBody() throws Exception {
        AwsProxyResponse response = invoke("/definitely-missing-path", "GET", null);

        assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.getBody()).isNotBlank();
        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(jsonNode.path("success").asBoolean()).isFalse();
        assertThat(jsonNode.path("error").path("code").asText()).isNotBlank();
    }

    @Test
    void signInFailureRespondsWithErrorBody() throws Exception {
        AwsProxyResponse response = invoke("/api/users/signin", "POST", "{}");

        assertThat(response.getStatusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.getBody()).isNotBlank();
        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.getBody());
        assertThat(jsonNode.path("success").asBoolean()).isFalse();
        assertThat(jsonNode.path("error").path("code").asText()).isNotBlank();
    }

    private AwsProxyResponse invoke(String path, String method, String body) throws Exception {
        HttpApiV2ProxyRequest request = new HttpApiV2ProxyRequest();
        request.setVersion("2.0");
        request.setRouteKey(method + " " + path);
        request.setRawPath(path);
        request.setRawQueryString("");
        request.setHeaders(Collections.singletonMap("content-type", "application/json"));
        request.setQueryStringParameters(Collections.emptyMap());
        request.setCookies(Collections.emptyList());
        request.setStageVariables(Collections.emptyMap());
        request.setBody(body);

        HttpApiV2HttpContext http = new HttpApiV2HttpContext();
        http.setMethod(method);
        http.setPath(path);

        HttpApiV2ProxyRequestContext context = new HttpApiV2ProxyRequestContext();
        context.setTime(Instant.now().toString());
        context.setHttp(http);
        context.setStage("$default");
        request.setRequestContext(context);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new StreamLambdaHandler().handleRequest(
                new ByteArrayInputStream(OBJECT_MAPPER.writeValueAsBytes(request)),
                output,
                new DummyContext()
        );

        return OBJECT_MAPPER.readValue(output.toByteArray(), AwsProxyResponse.class);
    }

    private SpringBootLambdaContainerHandler<?, ?> extractContainerHandler() throws Exception {
        Field field = StreamLambdaHandler.class.getDeclaredField("HANDLER");
        field.setAccessible(true);
        return (SpringBootLambdaContainerHandler<?, ?>) field.get(null);
    }

    private static class DummyContext implements Context {

        @Override
        public String getAwsRequestId() {
            return "dummy";
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
                    // no-op
                }

                @Override
                public void log(byte[] message) {
                    // no-op
                }
            };
        }
    }
}
