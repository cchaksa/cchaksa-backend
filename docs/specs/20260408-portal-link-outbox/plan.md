# Plan

## Architecture / Layering
- Domain impact: ScrapeJob 에 worker 결과만 기록하는 보조 메서드 추가, 최종 상태는 포털 후처리 완료 시에만 SUCCEEDED 로 전환.
- Application orchestration: ScrapeResultCallbackService 는 afterCommit 이벤트만 발행하고 상태 전환은 PortalCallbackPostProcessor 의 REQUIRES_NEW 트랜잭션에서 수행한다.
- Infrastructure touchpoints: PortalCallbackPostProcessor 가 ScrapeJobRepository/PortalSyncService 를 단일 TransactionTemplate 으로 묶어 atomic 하게 실행, 실패 시 별도 트랜잭션에서 실패 상태 기록.
- Global/config changes: 없음 (기존 hikari/open-in-view 변경은 완료), 관측 지표만 카운터 기반으로 확장.

## Data / Transactions
- Repositories touched: `ScrapeJobRepository`, `StudentRepository`, `UserRepository`, `StudentCourseRepository`, `StudentCourseBulkRepository`.
- Transaction scope: PortalSyncService 전 구간(포털 초기화+학업 동기화+최종 상태 반영)을 AFTER_COMMIT 리스너 안 단일 REQUIRES_NEW 트랜잭션으로 묶고, 실패 시 동일 트랜잭션을 롤백한다.
- Consistency expectations: 성공 시에만 `ScrapeJobStatus` 가 SUCCEEDED 로 커밋, 실패 시 job 은 FAILED 로 전환되고 Student/수강 데이터는 롤백되어 재시도 시 중복이 없다.

## Testing Strategy
- Domain tests: ScrapeJob 보조 메서드(TTL 없는 상태 기록)가 상태를 바꾸지 않는지 검증.
- Application tests: ScrapeResultCallbackService 는 성공 콜백 후에도 상태가 변하지 않고 이벤트 payload 에 finishedAt/queuedAge 가 실리는지 확인.
- Integration/tests: PortalCallbackPostProcessor 는 성공 시 job 이 SUCCEEDED 로 전환되고 실패 시 FAILED 가 되는지, Student 중복 재시도 시 unique 제약이 발생하지 않는지 H2 기반으로 검증.
- Additional commands: `./gradlew test`.

## Rollout Considerations
- Backward compatibility: API 응답 202 유지, Job 상태 해석만 변경(최종 완료 후에만 SUCCEEDED) 되었음을 고객 공지 필요.
- Observability / metrics: `scrape.job.callback.postprocess.fail` reason 확장 + `scrape.job.callback.postprocess.job_failed` 신규 카운터로 실패 Job 수 추적.
- Feature flags / toggles: 없음, 모든 환경에 동일 로직 적용.
