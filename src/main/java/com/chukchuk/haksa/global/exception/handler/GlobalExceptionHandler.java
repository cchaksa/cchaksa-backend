package com.chukchuk.haksa.global.exception.handler;

import com.chukchuk.haksa.global.common.response.ErrorResponse;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.BaseException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 비즈니스 예외(대부분 4xx) → 로깅 없음, 응답만 */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req) {
        // 분류 정확도만 개선 (이벤트 전송 X)
        Sentry.setTag("error.code", ex.getCode());
        Sentry.setFingerprint(List.of("BASE_EXCEPTION", ex.getCode()));

        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
    }

    /** 404 */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(
            org.springframework.web.servlet.NoHandlerFoundException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.NOT_FOUND;
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.code(), ec.message(), null));
    }

    /** 400 계열 */
    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            BindException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_ARGUMENT;
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.code(), ec.message(), null));
    }

    /** 엔티티 없음 */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
    }

    /** 예상 못한 서버 오류 → Sentry 단일 캡처 */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        Sentry.captureException(ex); // 중복 방지: log.error는 메시지만
        log.error("[RuntimeException] ctx={}", ctx(req));
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.", null));
    }

    /** 최후 보루 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        Sentry.captureException(ex);
        log.error("[Unhandled] type={} ctx={}", ex.getClass().getSimpleName(), ctx(req));
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("E-UNHANDLED", "서버 오류가 발생했습니다.", null));
    }

    // ===== helper =====
    private String ctx(HttpServletRequest req) {
        String base = nvl(req.getRequestURI());
        String q = req.getQueryString();
        String full = (q == null || q.isBlank()) ? base : base + "?" + q;
        String uri = LogSanitizer.clean(full);
        String method = nvl(req.getMethod());
        String traceId = nvl(MDC.get("traceId"));
        String spanId  = nvl(MDC.get("spanId"));
        String ip = LogSanitizer.clean(firstIp(nvl(req.getHeader("X-Forwarded-For")), nvl(req.getRemoteAddr())));
        return "method=%s uri=%s ip=%s traceId=%s spanId=%s"
                .formatted(method, uri, ip, traceId, spanId);
    }

    private String firstIp(String xff, String remote) {
        if (!xff.isBlank()) return xff.split(",")[0].trim();
        return remote;
    }

    private String nvl(String s) { return s == null ? "" : s; }
}