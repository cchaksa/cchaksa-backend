# Plan

## Architecture / Layering
- Domain impact: ScrapeJob 은 성공 콜백 저장 시 `recordWorkerResult` 까지만 수행하며, 포털 동기화가 끝난 뒤에만 `markSucceeded` 혹은 실패 시 `markFailed` 된다.
- Application orchestration: ScrapeResultCallbackService 는 서명 검증/결과 적재/이벤트 발행까지만 수행하고, PortalCallbackPostProcessor 가 REQUIRES_NEW 트랜잭션에서 포털 연동 후 최종 상태를 확정한다.
- Infrastructure touchpoints: PortalCallbackPostProcessor 는 ScrapeJobRepository/PortalSyncService 를 묶어 실행하되, 실패 시 Job 상태 수정 없이 로그/메트릭만 남긴다.
- Global/config changes: Async executor(`portalCallbackExecutor`) 추가, callback/후처리 단계별 관측 지표 확장.

## Data / Transactions
- Repositories touched: `ScrapeJobRepository`, `StudentRepository`, `UserRepository`, `StudentCourseRepository`, `StudentCourseBulkRepository`.
- Transaction scope: `/internal/scrape-results` 트랜잭션은 job 상태/결과 저장까지만 책임지고 즉시 커밋한다. PortalSyncService 는 AFTER_COMMIT 이벤트의 REQUIRES_NEW 트랜잭션에서 실행되며 실패 시 롤백되지만 Job 상태는 유지된다.
- Consistency expectations: 성공 콜백이 수신되면 항상 `ScrapeJobStatus.SUCCEEDED` 로 커밋되며, 후처리 실패는 로그/메트릭 기반으로만 노출된다.

## Testing Strategy
- Domain tests: ScrapeJob `markSucceeded`/`recordWorkerResult` 조합이 중복 호출에도 idempotent 한지 검증.
- Application tests: ScrapeResultCallbackService 는 성공 콜백에서 즉시 SUCCEEDED 가 되며, duplicate 요청/FAILED 상태에서도 idempotent 하게 처리되는지 확인.
- Integration/tests: PortalCallbackPostProcessor 는 성공 시 포털 동기화를 실행하고 실패 시에도 Job 상태가 변하지 않는지, shadow 시나리오(중복 callback)에서 타임아웃 없이 응답하는지 검증.
- Additional commands: `./gradlew test`.

## Rollout Considerations
- Backward compatibility: API 응답 202 유지, 단 callback 응답 속도가 빨라지므로 워커 timeout 설정과 맞춰 운영 확인 필요.
- Observability / metrics: `scrape.job.callback.persisted`, `scrape.job.callback.duplicate`, `scrape.job.callback.postprocess.fail` 등 새로운 카운터를 대시보드/알림에 연동한다.
- Feature flags / toggles: 없음, 모든 환경에 동일 로직 적용.
