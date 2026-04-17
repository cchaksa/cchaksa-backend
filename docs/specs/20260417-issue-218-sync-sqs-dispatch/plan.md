# Plan

## Architecture / Layering
- Domain impact: 없음. 기존 `ScrapeJob`, `ScrapeJobOutbox` 상태 전이 메서드를 재사용한다.
- Application orchestration:
  - `PortalLinkJobService`가 job/outbox 저장 후 동기 publish를 직접 orchestration한다.
  - 기존 `ScrapeJobOutboxAfterCommitExecutor`는 제거하거나 더 이상 사용하지 않도록 정리한다.
  - publish 후 상태 반영은 짧은 트랜잭션 경계로 분리한다.
- Infrastructure touchpoints:
  - `ScrapeJobPublisher`(`SqsScrapeJobPublisher`)는 그대로 사용한다.
  - 필요 시 outbox/job 상태 반영용 application tx service 추가
- Global/config changes:
  - `ScrapeJobDispatchConfig`의 executor/scheduler bean 정리
  - `@Scheduled` dispatcher 의존 제거 여부 반영

## Data / Transactions
- Repositories touched:
  - `ScrapeJobRepository`
  - `ScrapeJobOutboxRepository`
- Transaction scope:
  - 1차 tx: job/outbox 저장
  - tx 외부: SQS publish
  - 2차 tx: publish 성공/실패 상태 반영
- Consistency expectations:
  - 저장 자체는 원자적
  - publish 성공 시 job=`RUNNING`, outbox=`SENT`
  - publish 실패 시 재처리 가능 상태/실패 정보가 남아야 함

## Testing Strategy
- Domain tests:
  - 상태 전이 기존 규칙 회귀 확인
- Application tests:
  - `PortalLinkJobServiceUnitTests`에 동기 publish 성공/실패 시나리오 추가
  - 기존 after-commit executor 테스트는 제거/대체
- Integration/API tests:
  - `PortalLinkControllerApiIntegrationTest` 회귀 확인
- Additional commands:
  - `./gradlew test --tests "com.chukchuk.haksa.application.portal.PortalLinkJobServiceUnitTests"`
  - `./gradlew test --tests "com.chukchuk.haksa.application.portal.ScrapeJobOutboxDispatcherUnitTests"`
  - `./gradlew test`

## Rollout Considerations
- Backward compatibility: 외부 API 계약은 유지된다.
- Observability / metrics:
  - publish start/success/fail 로그 유지 또는 보강
  - `jobId`, `outboxId`, `queueMessageId`, `attempt` 기준 추적 가능해야 함
- Feature flags / toggles: 없음
