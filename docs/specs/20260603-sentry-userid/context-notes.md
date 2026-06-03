# feat/253 Sentry userId 추적 보강 컨텍스트 노트

## 2026-06-03

- 현재 worktree는 detached HEAD였고 `feat/253` 브랜치를 새로 만들었다.
- 변경 전 기준 `./gradlew test`는 통과했다. 컴파일 경고는 기존 경고로 보고 진행한다.
- userId 누락의 중심 원인은 인증 필터 밖 흐름이다. `/internal/scrape-results`, outbox dispatch, maintenance는 job을 조회해야 userId를 알 수 있다.
- job 조회 전 단계의 invalid signature, malformed body 같은 오류는 userId를 알 수 없다. 이 케이스는 body hash나 jobId 힌트만 남기는 정책을 유지한다.
- API 응답 형태와 OpenAPI 문서는 변경하지 않는다.
- `SentryMdcContext`는 실행 구간 동안 `userId`, `jobId`, `outboxId`, `operationType`, `workerRequestId`를 MDC에 넣고 종료 시 제거한다. Sentry user context도 같은 구간에서 설정한다.
- `ScrapeResultCallbackService`는 receipt에서 userId를 얻은 뒤 현재 request attribute에도 context를 저장한다. 전역 예외 처리기가 Sentry capture 직전에 이 context를 다시 MDC로 올려 직접 capture 이벤트에서도 tag가 유지된다.
- `ScrapeJobOutboxPublishCandidate`에 `userId`와 `operationType`을 포함했다. publish retry와 markFailed는 같은 scope 안에서 실행된다.
- stale reconcile은 job/outbox 단위 timeout 로그를 scope 안에서 남긴다.
- 중앙 예외 처리에서 capture되는 포털 실패는 내부 `ERROR` 로그를 `WARN`으로 낮춰 SentryAppender 중복 이벤트를 줄였다. outbox dead, dispatch fail, Lambda maintenance fail처럼 별도 운영 이벤트가 필요한 `ERROR`는 유지했다.
- 최종 검증은 `./gradlew test --rerun-tasks`로 수행했다.
- 커밋은 공통 MDC/Sentry 기반, 포털 작업 context 전파, 사용자 오류 Sentry 전송 축소, 작업 문서의 네 단위로 분리한다.
- Gemini 리뷰 중 Sentry user context 해제와 nullable context 변환 지적을 반영했다. Sentry user는 `pushScope()` 토큰으로 격리해 close 시 이전 scope가 복구되게 하고, UUID/Enum 변환은 `SentryMdcContext.from(...)`으로 중앙화한다.
