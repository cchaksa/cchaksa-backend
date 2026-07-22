# Sentry Event Classification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Checked items record completed work.

**Goal:** Keep every actionable user event while making Sentry issues searchable, readable, and free from known same-request duplicate log events.

**Architecture:** `GlobalExceptionHandler` remains the single capture point for handled web exceptions. A small Logback filter rejects only known duplicate web-request logs from the exception handler and Hibernate, while a Spring-managed Sentry callback normalizes handled domain exception titles without changing API responses or fingerprints.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Sentry Java 8.16.0, Logback, JUnit 5, AssertJ.

## Global Constraints

- Preserve every event for actionable errors such as `G02`; do not sample by user.
- Never include raw student codes, email addresses, tokens, SQL, IPs, UUIDs, trace IDs, or span IDs in issue titles.
- Keep user-specific values as searchable event tags, never as fingerprint values.
- Keep dev and prod Sentry environments separate.
- Follow test-first red-green cycles and split commits into small logical units near 30 changed lines when practical.

---

### Task 1: Normalize searchable Sentry tags

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/global/logging/sentry/SentryMdcTagBinder.java`
- Modify: `src/main/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolver.java`
- Modify: `src/main/resources/logback-spring.xml`
- Test: `src/test/java/com/chukchuk/haksa/global/logging/sentry/SentryMdcTagBinderTest.java`
- Test: `src/test/java/com/chukchuk/haksa/domain/graduation/policy/GraduationMajorResolverTest.java`
- Test: `src/test/java/com/chukchuk/haksa/global/logging/LogbackConfigTest.java`

**Interfaces:**
- Consumes: existing MDC values and `HashUtil.sha256Short(String)`.
- Produces: `studentCodeHash`, `admissionYear`, `departmentId`, `secondaryDepartmentId`, `majorType`, and `traceId` Sentry tags.

- [x] Write failing tests that require hashed student identification and camelCase graduation tags.
- [x] Run `./gradlew test --tests '*SentryMdcTagBinderTest' --tests '*GraduationMajorResolverTest' --tests '*LogbackConfigTest'` and verify the new assertions fail.
- [x] Replace raw `student_code` MDC with `studentCodeHash` and normalize graduation tag names at the throw site.
- [x] Align the binder and both Logback Sentry appenders with the normalized tag list.
- [x] Re-run the focused tests and verify they pass.
- [x] Commit with `310 fix: Sentry 사용자 문맥 태그 정규화`.

### Task 2: Filter expected client errors without hiding actionable errors

**Files:**
- Modify: `src/main/java/com/chukchuk/haksa/global/exception/handler/GlobalExceptionHandler.java`
- Test: `src/test/java/com/chukchuk/haksa/global/exception/handler/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `BaseException.getCode()` and request URI for contextual entity-not-found handling.
- Produces: `shouldReportBaseException(BaseException)` and `shouldReportEntityNotFound(EntityNotFoundException, HttpServletRequest)` decisions.

- [x] Add failing tests that exclude lecture-evaluation client errors and stale refresh-token user lookups.
- [x] Add a regression assertion that `G02` and system scrape failures remain reportable.
- [x] Run `./gradlew test --tests '*GlobalExceptionHandlerTest'` and verify the new exclusions fail.
- [x] Extend expected error-code filtering to all `BaseException` subtypes and add URI-scoped `U01` filtering only for `/api/auth/refresh`.
- [x] Re-run the focused test and verify it passes.
- [x] Commit with `310 fix: 예상 가능한 Sentry 오류 제외`.

### Task 3: Normalize issue titles while preserving exceptions

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/global/logging/sentry/SentryEventTitleCallback.java`
- Test: `src/test/java/com/chukchuk/haksa/global/logging/sentry/SentryEventTitleCallbackTest.java`
- Modify: `src/main/java/com/chukchuk/haksa/global/exception/handler/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: event tags `error.code`, `error.title`, and the processed Sentry exception list.
- Produces: a `SentryOptions.BeforeSendCallback` Spring bean that rewrites only the displayed outer exception type/value and retains its stack trace.

- [x] Write failing callback tests for `[G02] 졸업요건 데이터 없음`, non-domain exceptions, and missing exception lists.
- [x] Run `./gradlew test --tests '*SentryEventTitleCallbackTest'` and verify compilation or assertions fail because the callback does not exist.
- [x] Add the callback bean and set `error.title` plus `operation` tags at the handler capture boundary.
- [x] Keep fingerprints based on stable error type and code, never user tags.
- [x] Re-run title and handler tests and verify they pass.
- [x] Commit with `310 feat: Sentry 이슈 제목 정규화`.

### Task 4: Remove known same-request Logback duplicates

**Files:**
- Create: `src/main/java/com/chukchuk/haksa/global/logging/sentry/SentryDuplicateEventFilter.java`
- Test: `src/test/java/com/chukchuk/haksa/global/logging/sentry/SentryDuplicateEventFilterTest.java`
- Modify: `src/main/java/com/chukchuk/haksa/global/logging/filter/MdcUserEnricherFilter.java`
- Modify: `src/main/resources/logback-spring.xml`
- Test: `src/test/java/com/chukchuk/haksa/global/logging/LogbackConfigTest.java`

**Interfaces:**
- Consumes: MDC marker `sentryManagedRequest` and `ILoggingEvent.getLoggerName()`.
- Produces: `DENY` only for managed web-request logs from `GlobalExceptionHandler` and `org.hibernate.*`; all other events return `NEUTRAL`.

- [x] Write failing filter tests for managed duplicate loggers, unmanaged/background logs, and unrelated application loggers.
- [x] Run `./gradlew test --tests '*SentryDuplicateEventFilterTest' --tests '*LogbackConfigTest'` and verify the new behavior fails.
- [x] Add and clean up the request-scoped MDC marker in `MdcUserEnricherFilter`.
- [x] Add the filter to dev and prod Sentry appenders.
- [x] Re-run focused tests and verify they pass.
- [x] Commit with `310 fix: 동일 요청 Sentry 로그 중복 차단`.

### Task 5: Verify integrated behavior

**Files:**
- Modify only if verification exposes a requirement-linked defect.

**Interfaces:**
- Consumes: all changes from Tasks 1 through 4.
- Produces: verified #310 acceptance criteria.

- [x] Run `./gradlew test --tests '*GlobalExceptionHandlerTest' --tests '*Sentry*Test' --tests '*LogbackConfigTest'`.
- [x] Run the full `./gradlew test --stacktrace --no-daemon` suite.
- [x] Inspect `git diff --check` and `git status --short`.
- [x] Confirm `G02` remains reportable, expected client errors are excluded, raw student code is absent from tags, and only known duplicate loggers are denied.
- [x] Review the final diff for unrelated changes and PII exposure.
