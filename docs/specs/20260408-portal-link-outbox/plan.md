# Plan

## Architecture / Layering
- Domain impact: Job 상태 규칙은 동일, 도메인 변경 없음.
- Application orchestration: PortalLinkJobService.afterCommit 훅을 스케줄러 기반 비동기로 위임, ScrapeJobOutboxDispatcher 는 큐 전용 async executor 사용 및 REQUIRES_NEW 최소화.
- Infrastructure touchpoints: ScrapeJobOutboxDispatcher 가 사용하는 repository (ScrapeJobOutboxRepository) 에 대한 커넥션 사용 최적화, gauge 등록 시 lazy 데이터 소스 접근으로 교체.
- Global/config changes: `application.yml` profile group에서 shadow/dev/prod 에 hikari pool size, connection-timeout, open-in-view 설정, outbox executor 설정 추가.

## Data / Transactions
- Repositories touched: `ScrapeJobOutboxRepository`, `PortalLinkJobRepository`, metrics gauge용 count 쿼리.
- Transaction scope: PortalLinkJobService 트랜잭션은 job insert 까지, 디스패치는 별도 async executor + REQUIRES_NEW 트랜잭션에서 DB 접근 후 즉시 종료.
- Consistency expectations: outbox 행이 커밋되면 eventual하게 SQS 로 이동, 실패 시 executor 재시도 혹은 scheduler fallback.

## Testing Strategy
- Domain tests: 상태 전이 규칙은 기존 테스트로 커버, 변경 없음.
- Application tests: PortalLinkJobService/ScrapeJobOutboxDispatcher 통합 테스트에서 connection 부족 상황을 모킹하여 afterCommit 비동기 경로가 재시도하는지 검증.
- Integration/API tests: WebFlux controller test 로 `/portal/link` 호출 후 dispatcher mock 이 호출되는지 확인.
- Additional commands: `./gradlew test`, 필요 시 `./gradlew :application:test --tests "*PortalLink*"`.

## Rollout Considerations
- Backward compatibility: API 응답 202 유지, 오직 내부 비동기 경로만 수정.
- Observability / metrics: gauge 제거 후 Micrometer counter/summary 로 대체, 실패 시 로그에 `scrape.outbox.dispatch.retry` 이벤트 추가.
- Feature flags / toggles: shadow 환경만 우선 적용 가능한 `scraping.outbox.async.enabled` 플래그 추가, 추후 dev/prod 확대.
