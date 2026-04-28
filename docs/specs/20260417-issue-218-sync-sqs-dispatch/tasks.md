# Tasks

## Checklist
- [x] Domain tests/회귀 범위 확인
- [x] Application layer에서 동기 publish orchestration으로 전환
- [x] Infrastructure layer에서 불필요한 async dispatch 구성 제거
- [x] Global/config에서 executor/scheduler 의존 정리
- [x] Lambda 부팅 시 `TaskScheduler` 부재로 인한 reporter 빈 생성 실패 방지
- [x] 요청 흐름 안 SQS 발행에 짧은 bounded retry 추가
- [x] API/controller 회귀 확인
- [x] Documentation/spec 업데이트

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "com.chukchuk.haksa.application.portal.PortalLinkJobServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.PortalLinkJobTxServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.ScrapeJobOutboxDispatcherUnitTests"` | PASS | 2026-04-17 |
| 2 | `./gradlew test` | PASS | 2026-04-17 |
| 3 | `rg -n "ScrapeJobOutboxAfterCommitExecutor|after_commit|scrapeOutboxDispatchExecutor|scrapeOutboxRetryScheduler" src/main/java src/test/java` | PASS (no matches) | 2026-04-17 |
| 4 | `./gradlew test --tests "com.chukchuk.haksa.application.portal.ScrapeResultCallbackServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.PortalCallbackPostProcessorTests" --tests "com.chukchuk.haksa.infrastructure.portal.client.ScrapeResultStoreClientTests"` | PASS | 2026-04-17 |
| 5 | `./gradlew test --tests "com.chukchuk.haksa.application.portal.ScrapeJobOutboxMetricsReporterTests" --tests "com.chukchuk.haksa.application.portal.PortalLinkJobServiceUnitTests" --tests "com.chukchuk.haksa.application.portal.ScrapeResultCallbackServiceUnitTests"` | PASS | 2026-04-17 |
| 6 | `./gradlew test` | PASS | 2026-04-17 |
| 7 | `./gradlew test --tests "com.chukchuk.haksa.application.portal.ScrapeJobOutboxDispatcherUnitTests"` | PASS | 2026-04-26 |
| 8 | `./gradlew test` | PASS | 2026-04-26 |

## Notes
- Observation: 기존 문제는 `scrape.outbox.dispatch.after_commit.start` 이후 publish 로그가 남지 않는 Lambda in-memory async 경계에서 발생했다.
- Observation: 추가 리뷰 반영으로 callback의 HMAC 검증 순서, raw checksum 검증, jobId path segment 검증을 강화했다.
- Observation: Lambda 배포 실패 로그 기준 `ScrapeJobOutboxMetricsReporter`가 `TaskScheduler` 빈 부재로 앱 부팅을 깨뜨렸고, reporter는 조건부 빈으로 완화해야 한다.
- Observation: SQS 발행 transient failure는 요청 안에서 최대 2회 짧게 재시도하고, DB `attempt_count`는 outbox dispatch 실행 단위로만 증가시킨다.
