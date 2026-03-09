package com.chukchuk.haksa.global.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.chukchuk.haksa.ChukchukHaksaApplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER;

    static {
        System.setProperty("spring.main.web-application-type", "servlet");
        try {
            HANDLER = SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(
                    ChukchukHaksaApplication.class,
                    LambdaProfiles.resolveActiveProfiles()
            );
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        HANDLER.proxyStream(input, output, context);
    }
}
