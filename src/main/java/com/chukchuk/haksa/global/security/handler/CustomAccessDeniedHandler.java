package com.chukchuk.haksa.global.security.handler;

import com.chukchuk.haksa.global.common.response.ErrorResponse;
import com.chukchuk.haksa.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 403 Forbidden
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        log.info("Access denied: {}, path: {}", errorCode.name(), request.getRequestURI());

        response.setStatus(errorCode.status().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse error = ErrorResponse.of(errorCode.code(), errorCode.message());
        response.getWriter().write(objectMapper.writeValueAsString(error));
        response.getWriter().flush();
    }
}
