package com.chukchuk.haksa.global.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.chukchuk.haksa.ChukchukHaksaApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

public final class LambdaSpringContext {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER = buildHandler();

    private LambdaSpringContext() {
    }

    public static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler() {
        return HANDLER;
    }

    public static ApplicationContext applicationContext() {
        Object rootContext = HANDLER.getServletContext()
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (rootContext instanceof ApplicationContext applicationContext) {
            return applicationContext;
        }
        throw new IllegalStateException("Root Spring application context is not available");
    }

    private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> buildHandler() {
        System.setProperty("spring.main.web-application-type", "servlet");
        try {
            SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler =
                    new SpringBootProxyHandlerBuilder<HttpApiV2ProxyRequest>()
                            .defaultHttpApiV2Proxy()
                            .initializationWrapper(new InitializationWrapper())
                            .servletApplication()
                            .springBootApplication(ChukchukHaksaApplication.class)
                            .profiles(LambdaProfiles.resolveActiveProfiles())
                            .buildAndInitialize();
            handler.getContainerConfig().addBinaryContentTypes(
                    "text/javascript",
                    "application/javascript",
                    "text/css",
                    "image/png",
                    "image/svg+xml",
                    "font/woff2"
            );
            return handler;
        } catch (ContainerInitializationException e) {
            throw new IllegalStateException("Could not initialize Spring Boot application", e);
        }
    }
}
