// Sentry 이벤트 검색을 위한 MDC 컨텍스트 스코프 헬퍼

package com.chukchuk.haksa.global.logging.sentry;

import io.sentry.ISentryLifecycleToken;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 스크래핑 작업 식별 정보를 MDC와 HTTP 요청 사이에 전달한다. */
public final class SentryMdcContext {

  private static final String USER_ID = "userId";
  private static final String JOB_ID = "jobId";
  private static final String OUTBOX_ID = "outboxId";
  private static final String OPERATION_TYPE = "operationType";
  private static final String WORKER_REQUEST_ID = "workerRequestId";
  private static final String ATTR_PREFIX = SentryMdcContext.class.getName() + ".";

  private SentryMdcContext() {}

  /**
   * 지정한 MDC 문맥에서 작업을 실행한다.
   *
   * @param context 적용할 문맥
   * @param action 실행할 작업
   */
  public static void run(Context context, Runnable action) {
    try (MdcScope ignored = open(context)) {
      action.run();
    }
  }

  /**
   * 지정한 MDC 문맥에서 값을 계산해 반환한다.
   *
   * @param <T> 공급자가 계산할 값의 유형
   * @param context 적용할 문맥
   * @param supplier 실행할 값 공급자
   * @return 지정한 문맥 안에서 공급자가 계산한 값
   */
  public static <T> T supply(Context context, Supplier<T> supplier) {
    try (MdcScope ignored = open(context)) {
      return supplier.get();
    }
  }

  /**
   * 지정한 값을 적용한 MDC 범위를 연다.
   *
   * @param context 적용할 문맥
   * @return 닫을 때 이전 MDC와 Sentry 사용자 문맥을 복원하는 scope
   */
  public static MdcScope open(Context context) {
    return new MdcScope(context);
  }

  /**
   * 스크래핑 작업 식별값으로 MDC 문맥을 생성한다.
   *
   * @param userId 사용자 식별자
   * @param jobId 작업 식별자
   * @param outboxId 아웃박스 식별자
   * @param operationType 작업 유형
   * @param workerRequestId 워커 요청 식별자
   * @return null 식별자는 제외하고 문자열로 변환한 MDC 문맥
   */
  public static Context from(
      UUID userId, String jobId, String outboxId, Enum<?> operationType, String workerRequestId) {
    return new Context(
        userId != null ? userId.toString() : null,
        jobId,
        outboxId,
        operationType != null ? operationType.name() : null,
        workerRequestId);
  }

  /**
   * 현재 HTTP 요청에 스크래핑 추적 문맥을 저장한다.
   *
   * @param context 적용할 문맥
   */
  public static void bindToCurrentRequest(Context context) {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletAttributes) {
      bindToRequest(servletAttributes.getRequest(), context);
    }
  }

  /**
   * 지정한 HTTP 요청에 값이 있는 스크래핑 추적 식별자만 저장한다.
   *
   * @param request 스크래핑 추적 헤더를 읽을 HTTP 요청
   * @param context 적용할 문맥
   */
  public static void bindToRequest(HttpServletRequest request, Context context) {
    if (request == null || context == null) {
      return;
    }
    setAttribute(request, USER_ID, context.userId());
    setAttribute(request, JOB_ID, context.jobId());
    setAttribute(request, OUTBOX_ID, context.outboxId());
    setAttribute(request, OPERATION_TYPE, context.operationType());
    setAttribute(request, WORKER_REQUEST_ID, context.workerRequestId());
  }

  /**
   * HTTP 요청에 저장된 값으로 MDC 범위를 연다.
   *
   * @param request MDC에 적용할 추적 속성이 저장된 HTTP 요청
   * @return 요청 문맥을 적용한 scope이며 저장된 값이 없으면 아무 작업도 하지 않는 scope
   */
  public static MdcScope openFromRequest(HttpServletRequest request) {
    Context context = contextFromRequest(request);
    if (context == null) {
      return MdcScope.noop();
    }
    return open(context);
  }

  /**
   * 로그와 Sentry 이벤트를 같은 스크래핑 작업에 연결할 추적 식별자를 전달한다.
   *
   * @param userId 사용자 식별자
   * @param jobId 작업 식별자
   * @param outboxId 아웃박스 식별자
   * @param operationType 작업 유형
   * @param workerRequestId 워커 요청 식별자
   */
  public record Context(
      String userId, String jobId, String outboxId, String operationType, String workerRequestId) {}

  /** 적용 전 MDC 값을 복원할 수 있는 문맥 범위를 관리한다. */
  public static final class MdcScope implements AutoCloseable {
    private final Map<String, String> previousValues = new LinkedHashMap<>();
    private final boolean noop;
    private final ISentryLifecycleToken sentryScopeToken;

    private MdcScope(Context context) {
      this.noop = false;
      this.sentryScopeToken = hasText(context.userId()) ? Sentry.pushScope() : null;
      put(USER_ID, context.userId());
      put(JOB_ID, context.jobId());
      put(OUTBOX_ID, context.outboxId());
      put(OPERATION_TYPE, context.operationType());
      put(WORKER_REQUEST_ID, context.workerRequestId());

      if (hasText(context.userId())) {
        User sentryUser = new User();
        sentryUser.setId(context.userId());
        Sentry.setUser(sentryUser);
      }
    }

    private MdcScope() {
      this.noop = true;
      this.sentryScopeToken = null;
    }

    private static MdcScope noop() {
      return new MdcScope();
    }

    private void put(String key, String value) {
      previousValues.put(key, MDC.get(key));
      if (hasText(value)) {
        MDC.put(key, value);
      } else {
        MDC.remove(key);
      }
    }

    @Override
    public void close() {
      if (noop) {
        return;
      }
      previousValues.forEach(
          (key, value) -> {
            if (value == null) {
              MDC.remove(key);
            } else {
              MDC.put(key, value);
            }
          });
      if (sentryScopeToken != null) {
        sentryScopeToken.close();
      }
    }

    private boolean hasText(String value) {
      return value != null && !value.isBlank();
    }
  }

  private static Context contextFromRequest(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    String userId = attribute(request, USER_ID);
    String jobId = attribute(request, JOB_ID);
    String outboxId = attribute(request, OUTBOX_ID);
    String operationType = attribute(request, OPERATION_TYPE);
    String workerRequestId = attribute(request, WORKER_REQUEST_ID);
    if (!hasText(userId)
        && !hasText(jobId)
        && !hasText(outboxId)
        && !hasText(operationType)
        && !hasText(workerRequestId)) {
      return null;
    }
    return new Context(userId, jobId, outboxId, operationType, workerRequestId);
  }

  private static void setAttribute(HttpServletRequest request, String key, String value) {
    if (hasText(value)) {
      request.setAttribute(ATTR_PREFIX + key, value);
    }
  }

  private static String attribute(HttpServletRequest request, String key) {
    Object value = request.getAttribute(ATTR_PREFIX + key);
    return value instanceof String stringValue ? stringValue : null;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
