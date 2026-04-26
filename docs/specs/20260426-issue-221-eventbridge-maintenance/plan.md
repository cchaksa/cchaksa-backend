# Plan

## Architecture / Layering
- Domain impact: 없음. 기존 scrape job 상태 전이 규칙과 refresh token 만료 규칙을 유지한다.
- Application orchestration:
  - maintenance task enum과 handler를 추가한다.
  - handler는 task별로 `ScrapeJobStaleReconciler`와 `RefreshTokenService`를 호출하고 affected count를 반환한다.
- Infrastructure touchpoints: 신규 외부 client나 DB schema 없음.
- Global/config changes:
  - `StreamLambdaHandler`가 raw input을 읽어 EventBridge Scheduler maintenance event면 Spring bean으로 라우팅한다.
  - maintenance event가 아니면 기존 HTTP proxy stream 경로로 넘긴다.

## Data / Transactions
- Repositories touched:
  - 기존 stale job reconcile repository 접근.
  - 기존 refresh token repository 삭제 메서드.
- Transaction scope:
  - 각 maintenance task service 내부의 기존 짧은 트랜잭션을 유지한다.
  - Lambda handler 자체에는 트랜잭션을 두지 않는다.
- Consistency expectations:
  - EventBridge 재시도/중복 실행에도 결과가 깨지지 않아야 한다.

## Testing Strategy
- Application tests:
  - known task가 올바른 service를 호출하고 affected count를 반환하는지 검증.
  - unknown task는 실패하는지 검증.
  - refresh token cleanup이 삭제 count를 반환하는지 검증.
- Global/Lambda tests:
  - EventBridge Scheduler event가 maintenance handler로 라우팅되는지 검증.
  - 일반 HTTP API event는 기존 proxy handler로 전달되는지 회귀 검증.
- Additional commands:
  - `./gradlew test --tests "com.chukchuk.haksa.application.maintenance.*"`
  - `./gradlew test --tests "com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest"`
  - `./gradlew test`

## Rollout Considerations
- Backward compatibility:
  - HTTP API event 처리 경로는 유지한다.
  - 기존 scheduler bean은 코드에서 사용하지 않도록 정리한다.
- Observability / metrics:
  - task, scheduled_at, affected_count, elapsed_ms를 maintenance 로그에 남긴다.
- Feature flags / toggles:
  - 별도 feature flag 없음.
  - Lambda 환경에서는 `SCRAPING_SCHEDULER_ENABLED=false`를 인프라에서 설정하는 것을 권장한다.
