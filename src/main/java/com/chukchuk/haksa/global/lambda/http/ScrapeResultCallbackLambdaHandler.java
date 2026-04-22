package com.chukchuk.haksa.global.lambda.http;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.chukchuk.haksa.global.lambda.LambdaSpringContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ScrapeResultCallbackLambdaHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        LambdaSpringContext.handler().proxyStream(input, output, context);
    }
}
