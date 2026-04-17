# Tasks

## Checklist
- [x] Domain tests/회귀 범위 확인
- [x] Application layer에서 동기 publish orchestration으로 전환
- [x] Infrastructure layer에서 불필요한 async dispatch 구성 제거
- [x] Global/config에서 executor/scheduler 의존 정리
- [x] API/controller 회귀 확인
- [x] Documentation/spec 업데이트

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "com.chukchuk.haksa.application.portal.PortalLinkJobServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.PortalLinkJobTxServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.ScrapeJobOutboxDispatcherUnitTests"` | PASS | 2026-04-17 |
| 2 | `./gradlew test` | PASS | 2026-04-17 |
| 3 | `rg -n "ScrapeJobOutboxAfterCommitExecutor|after_commit|scrapeOutboxDispatchExecutor|scrapeOutboxRetryScheduler" src/main/java src/test/java` | PASS (no matches) | 2026-04-17 |

## Notes
- Observation: 기존 문제는 `scrape.outbox.dispatch.after_commit.start` 이후 publish 로그가 남지 않는 Lambda in-memory async 경계에서 발생했다.
