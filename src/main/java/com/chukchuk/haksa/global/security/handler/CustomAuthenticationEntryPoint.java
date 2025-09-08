package com.chukchuk.haksa.global.security.handler;

import com.chukchuk.haksa.global.common.response.ErrorResponse;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 인증 실패 (401 Unauthorized)
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String EXCEPTION_ATTR = "exception";
    private static final String EXPIRED = "expired";
    private static final String INVALID = "invalid";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        String exceptionType = (String) request.getAttribute(EXCEPTION_ATTR);
        ErrorCode errorCode;
        if (EXPIRED.equals(exceptionType)) {
            errorCode = ErrorCode.TOKEN_EXPIRED;
        } else if (INVALID.equals(exceptionType)) {
            errorCode = ErrorCode.TOKEN_INVALID;
        } else {
            errorCode = ErrorCode.AUTHENTICATION_REQUIRED; // 토큰 없음 등 일반적인 인증 실패 응답
        }

        log.info("Authentication failed: {}, path: {}", errorCode.name(), request.getRequestURI());

        response.setStatus(errorCode.status().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse error = ErrorResponse.of(errorCode.code(), errorCode.message());
        response.getWriter().write(objectMapper.writeValueAsString(error));
        response.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + errorCode.name() + "\"");
        response.getWriter().flush();
    }
}
