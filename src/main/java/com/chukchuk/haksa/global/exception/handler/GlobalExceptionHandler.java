package com.chukchuk.haksa.global.exception.handler;

import com.chukchuk.haksa.global.common.response.ErrorResponse;
import com.chukchuk.haksa.global.exception.code.ErrorCode;
import com.chukchuk.haksa.global.exception.type.BaseException;
import com.chukchuk.haksa.global.exception.type.EntityNotFoundException;
import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import com.chukchuk.haksa.global.logging.sentry.SentryEventTitleCallback;
import com.chukchuk.haksa.global.logging.sentry.SentryMdcContext;
import com.chukchuk.haksa.global.logging.sentry.SentryMdcTagBinder;
import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.servlet.HandlerMapping;

/** 컨트롤러 예외를 공통 오류 응답으로 변환하고 예상하지 못한 오류를 Sentry에 기록한다. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private static final Set<String> EXPECTED_CLIENT_ERROR_CODES =
      Set.of(
          ErrorCode.SCRAPE_JOB_FAILED_RESULT.code(),
          ErrorCode.SCRAPE_JOB_NOT_COMPLETED.code(),
          ErrorCode.SCRAPE_JOB_NOT_FOUND.code(),
          ErrorCode.PORTAL_LOGIN_FAILED.code(),
          ErrorCode.PORTAL_ACCOUNT_LOCKED.code(),
          ErrorCode.ADMIN_CREDENTIALS_INVALID.code(),
          ErrorCode.ADMIN_CURRENT_PASSWORD_MISMATCH.code(),
          ErrorCode.ADMIN_PASSWORD_UNCHANGED.code(),
          ErrorCode.INVALID_CALLBACK_SIGNATURE.code(),
          ErrorCode.SCRAPE_INVALID_CALLBACK_REQUEST.code(),
          ErrorCode.SCRAPE_INVALID_S3_KEY.code(),
          ErrorCode.LECTURE_EVALUATION_NOT_REQUIRED.code(),
          ErrorCode.LECTURE_EVALUATION_COURSE_MISMATCH.code());

  /**
   * 애플리케이션 예외의 오류 코드와 HTTP 상태를 공통 오류 응답으로 변환한다.
   *
   * @param ex 응답으로 변환할 애플리케이션 예외
   * @param req 오류 문맥과 Sentry 태그를 구성할 HTTP 요청
   * @return 예외의 상태·코드·메시지를 포함한 오류 응답
   */
  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req) {

    if (shouldReportBaseException(ex)) {
      try (SentryMdcContext.MdcScope ignored = SentryMdcContext.openFromRequest(req)) {
        Sentry.withScope(
            scope -> {
              scope.setTag("error.type", "BASE_EXCEPTION");
              scope.setTag("error.code", ex.getCode());
              scope.setTag(SentryEventTitleCallback.ERROR_TITLE_TAG, sentryTitle(ex));
              scope.setTag("operation", sentryOperation(req));
              scope.setFingerprint(List.of("BASE_EXCEPTION", ex.getCode()));
              scope.setLevel(io.sentry.SentryLevel.WARNING); // 4xx 의미 유지

              // MDC → Sentry Tag 승격 (있을 때만)
              SentryMdcTagBinder.bind(scope);

              Sentry.captureException(ex);
            });
      }
    }

    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
  }

  /**
   * 매핑된 핸들러가 없는 요청을 표준 404 오류 응답으로 변환한다.
   *
   * @param ex Spring MVC가 전달한 핸들러 없음 예외
   * @param req 핸들러를 찾지 못한 HTTP 요청
   * @return 표준 not found 오류 응답
   */
  @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandler(
      org.springframework.web.servlet.NoHandlerFoundException ex, HttpServletRequest req) {
    ErrorCode ec = ErrorCode.NOT_FOUND;
    return ResponseEntity.status(ec.status()).body(ErrorResponse.of(ec.code(), ec.message(), null));
  }

  /**
   * 요청 본문·파라미터·검증 오류를 표준 잘못된 요청 응답으로 변환한다.
   *
   * @param ex 요청을 처리할 수 없게 만든 입력 예외
   * @param req 유효하지 않은 입력을 포함한 HTTP 요청
   * @return 표준 invalid argument 오류 응답
   */
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
    return ResponseEntity.status(ec.status()).body(ErrorResponse.of(ec.code(), ec.message(), null));
  }

  /**
   * 조회 대상이 없는 예외를 해당 상태와 코드의 오류 응답으로 변환한다.
   *
   * @param ex 조회 실패 원인과 오류 코드를 포함한 예외
   * @param req 조회를 요청한 HTTP 요청
   * @return 엔티티 없음 상태·코드·메시지를 포함한 오류 응답
   */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(
      EntityNotFoundException ex, HttpServletRequest req) {

    if (shouldReportEntityNotFound(ex, req)) {
      try (SentryMdcContext.MdcScope ignored = SentryMdcContext.openFromRequest(req)) {
        Sentry.withScope(
            scope -> {
              scope.setTag("error.type", "ENTITY_NOT_FOUND");
              scope.setTag("error.code", ex.getCode());
              scope.setTag(SentryEventTitleCallback.ERROR_TITLE_TAG, sentryTitle(ex));
              scope.setTag("operation", sentryOperation(req));
              scope.setFingerprint(List.of("ENTITY_NOT_FOUND", ex.getCode()));
              scope.setLevel(io.sentry.SentryLevel.WARNING);
              SentryMdcTagBinder.bind(scope);
              Sentry.captureException(ex);
            });
      }
    }

    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), null));
  }

  /**
   * 처리되지 않은 런타임 예외를 Sentry에 한 번 기록하고 서버 오류 응답으로 변환한다.
   *
   * @param ex 처리 중 발생한 예상하지 못한 런타임 예외
   * @param req 오류 문맥과 Sentry 태그를 구성할 HTTP 요청
   * @return 내부 구현 정보를 노출하지 않는 서버 오류 응답
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest req) {
    try (SentryMdcContext.MdcScope ignored = SentryMdcContext.openFromRequest(req)) {
      Sentry.withScope(
          scope -> {
            scope.setTag("operation", sentryOperation(req));
            SentryMdcTagBinder.bind(scope);
            Sentry.captureException(ex);
          });
    } // 중복 방지: log.error는 메시지만
    log.error("[RuntimeException] ctx={}", ctx(req));
    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.", null));
  }

  /**
   * 다른 핸들러가 처리하지 않은 예외를 기록하고 일반 서버 오류 응답으로 변환한다.
   *
   * @param ex 다른 핸들러에서 처리하지 못한 예외
   * @param req 오류 문맥과 Sentry 태그를 구성할 HTTP 요청
   * @return 내부 구현 정보를 노출하지 않는 일반 서버 오류 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
    try (SentryMdcContext.MdcScope ignored = SentryMdcContext.openFromRequest(req)) {
      Sentry.withScope(
          scope -> {
            scope.setTag("operation", sentryOperation(req));
            SentryMdcTagBinder.bind(scope);
            Sentry.captureException(ex);
          });
    }
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
    String spanId = nvl(MDC.get("spanId"));
    String ip =
        LogSanitizer.clean(
            firstIp(nvl(req.getHeader("X-Forwarded-For")), nvl(req.getRemoteAddr())));
    return "method=%s uri=%s ip=%s traceId=%s spanId=%s"
        .formatted(method, uri, ip, traceId, spanId);
  }

  private String firstIp(String xff, String remote) {
    if (!xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return remote;
  }

  private String nvl(String s) {
    return s == null ? "" : s;
  }

  private String sentryTitle(BaseException ex) {
    return "[%s] %s".formatted(ex.getCode(), ex.getMessage());
  }

  String sentryOperation(HttpServletRequest req) {
    Object pattern = req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String route = pattern instanceof String value ? value : "UNKNOWN";
    return "%s %s".formatted(nvl(req.getMethod()), route).trim();
  }

  boolean shouldReportBaseException(BaseException ex) {
    return !EXPECTED_CLIENT_ERROR_CODES.contains(ex.getCode());
  }

  boolean shouldReportEntityNotFound(EntityNotFoundException ex, HttpServletRequest req) {
    return !ErrorCode.USER_NOT_FOUND.code().equals(ex.getCode())
        || !"/api/auth/refresh".equals(req.getRequestURI());
  }
}
