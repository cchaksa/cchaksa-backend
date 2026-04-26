package com.chukchuk.haksa.global.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.chukchuk.haksa.ChukchukHaksaApplication;
import com.chukchuk.haksa.application.maintenance.MaintenanceTaskHandler;
import com.chukchuk.haksa.application.maintenance.MaintenanceTaskRequest;
import com.chukchuk.haksa.application.maintenance.MaintenanceTaskResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EVENTBRIDGE_SCHEDULER_SOURCE = "eventbridge.scheduler";
    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER;

    static {
        System.setProperty("spring.main.web-application-type", "servlet");
        try {
            HANDLER = new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                    .defaultHttpApiV2Proxy()
                    .initializationWrapper(new InitializationWrapper())
                    .servletApplication()
                    .springBootApplication(ChukchukHaksaApplication.class)
                    .profiles(LambdaProfiles.resolveActiveProfiles())
                    .buildAndInitialize();
            HANDLER.getContainerConfig().addBinaryContentTypes(
                    "text/javascript",
                    "application/javascript",
                    "text/css",
                    "image/png",
                    "image/svg+xml",
                    "font/woff2"
            );
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        byte[] payload = input.readAllBytes();
        if (isMaintenanceEvent(payload)) {
            handleMaintenanceEvent(payload, output);
            return;
        }

        HANDLER.proxyStream(new ByteArrayInputStream(payload), output, context);
    }

    private boolean isMaintenanceEvent(byte[] payload) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(payload);
            return EVENTBRIDGE_SCHEDULER_SOURCE.equals(jsonNode.path("source").asText())
                    && jsonNode.hasNonNull("task");
        } catch (IOException e) {
            return false;
        }
    }

    private void handleMaintenanceEvent(byte[] payload, OutputStream output) throws IOException {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(payload);
        MaintenanceTaskRequest request = new MaintenanceTaskRequest(
                jsonNode.path("source").asText(),
                jsonNode.path("task").asText(),
                jsonNode.path("scheduled_at").isMissingNode() ? null : jsonNode.path("scheduled_at").asText(null)
        );

        try {
            MaintenanceTaskResult result = maintenanceTaskHandler().handle(request);
            OBJECT_MAPPER.writeValue(output, result);
        } catch (RuntimeException e) {
            log.error("[BIZ] maintenance.task.failed task={} scheduledAt={} exceptionClass={} message={}",
                    request.task(), request.scheduledAt(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    private MaintenanceTaskHandler maintenanceTaskHandler() {
        return WebApplicationContextUtils.getRequiredWebApplicationContext(HANDLER.getServletContext())
                .getBean(MaintenanceTaskHandler.class);
    }
}
