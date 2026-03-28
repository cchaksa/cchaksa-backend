# Plan – Issue #198 CI Gate

## Architecture / Layering
- Domain impact: 없음 (코드/도메인 로직 미변경).
- Application orchestration: 없음.
- Infrastructure touchpoints: GitHub Actions 실행 환경(Hosted Ubuntu), Gradle 캐시(S3 backend 아님)만 사용.
- Global/config changes: `.github/workflows/ci.yml` 추가, Gradle 캐시 키(`~/.gradle` + Wrapper) 정의.

## Data / Transactions
- Repositories touched: `.github/workflows/ci.yml`.
- Transaction scope: CI Job 전체(Checkout → Cache → Gradle commands → Step summary).
- Consistency expectations: pull_request/push 이벤트마다 동일한 명령을 실행하고, 실패 시 즉시 Job 실패 처리.

## Testing Strategy
- Domain tests: N/A.
- Application tests: N/A.
- Integration/API tests: 새 workflow 내에서 `./gradlew test --stacktrace` 실행.
- Additional commands: 로컬에서 `./gradlew test`를 실행해 baseline 확인 (필수), workflow 문법은 `act` 대신 `workflow syntax` 검토로 대체.

## Rollout Considerations
- Backward compatibility: 기존 workflow와 독립적으로 동작, 다른 배포 workflow에 영향 없음.
- Observability / metrics: GitHub Actions Job Summary에 테스트/체크 단계 결과 요약, 실패 시 로그 링크 제공.
- Feature flags / toggles: 없음. Branch Protection에서 `ci-gradle` Job을 필수로 등록하도록 관리자에게 전달 (Clarify Follow-up #1).
