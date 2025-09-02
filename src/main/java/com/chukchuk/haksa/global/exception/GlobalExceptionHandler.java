package com.chukchuk.haksa.global.exception;

import com.chukchuk.haksa.global.common.response.ErrorResponse;
import com.chukchuk.haksa.global.logging.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 로깅 정책:
 * - WARN: 처리했지만 비정상(대부분 4xx) → 스택트레이스 금지
 * - ERROR: 작업 실패로 종료(5xx/예상불가) → 스택트레이스 필수
 * - 로그 컨텍스트는 요약/마스킹(URI+query만, 본문/헤더 원문 금지)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 우리 커스텀 예외(대부분 4xx) → WARN, 스택트레이스 X */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req) {
        log.warn("[BaseException] code={} msg={} ctx={}",
                ex.getCode(), ex.getMessage(), ctx(req));
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
    }

    /** 라우팅 미스매치(404) → WARN, 스택트레이스 X */
    @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(org.springframework.web.servlet.NoHandlerFoundException ex,
                                                         HttpServletRequest req) {
        ErrorCode ec = ErrorCode.NOT_FOUND;
        log.warn("[NoHandlerFound] path={} ctx={}", ex.getRequestURL(), ctx(req));
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.code(), ec.message(), null));
    }

    /** 잘못된 파라미터(400) → WARN, 스택트레이스 X */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_ARGUMENT;
        log.warn("[IllegalArgument] msg={} ctx={}", ex.getMessage(), ctx(req));
        return ResponseEntity.status(ec.status())
                .body(ErrorResponse.of(ec.code(), ex.getMessage(), null));
    }

    /** 엔티티 없음(대체로 404/409 등) → WARN, 스택트레이스 X */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
        log.warn("[EntityNotFound] code={} msg={} ctx={}", ex.getCode(), ex.getMessage(), ctx(req));
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
    }

    /** 예상 못한 서버 오류(5xx) → ERROR, 스택트레이스 O */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        log.error("[RuntimeException] msg={} ctx={}", ex.getMessage(), ctx(req), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.", null));
    }

    /** 최후 보루(그 밖의 모든 예외) → ERROR, 스택트레이스 O */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        log.error("[Unhandled] type={} msg={} ctx={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ctx(req), ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("E-UNHANDLED", "서버 오류가 발생했습니다.", null));
    }

    // ===== helper =====
    /** 요청 컨텍스트 요약(URI+query만, 마스킹 적용). traceId/spanId 포함 */
    private String ctx(HttpServletRequest req) {
        String base = nvl(req.getRequestURI());
        String q = req.getQueryString();
        String full = (q == null || q.isBlank()) ? base : base + "?" + q;
        String uri = LogSanitizer.clean(full);
        String method = nvl(req.getMethod());
        String traceId = nvl(MDC.get("traceId"));
        String spanId  = nvl(MDC.get("spanId"));
        return "method=%s uri=%s traceId=%s spanId=%s".formatted(method, uri, traceId, spanId);
    }
    private String nvl(String s) { return s == null ? "" : s; }
}