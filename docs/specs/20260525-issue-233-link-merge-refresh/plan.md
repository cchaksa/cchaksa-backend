# Plan Template

## Architecture / Layering
- Domain impact: 없음. 기존 `User.portalConnected` 상태와 scrape job operation type을 그대로 사용한다.
- Application orchestration: `PortalSyncService.syncWithPortal`에서 병합 후 active user 상태를 확인해 REFRESH 재동기화 경로로 전환한다.
- Infrastructure touchpoints: 없음.
- Global/config changes: 없음.

## Data / Transactions
- Repositories touched: 직접 추가 없음. 기존 `UserService`, `InitializePortalConnectionService`, `RefreshPortalConnectionService`, `SyncAcademicRecordService` 경로를 사용한다.
- Transaction scope: 기존 `PortalSyncService` 트랜잭션 안에서 분기만 변경한다.
- Consistency expectations: 사용자 병합 후 이미 연동 상태라면 포털 초기화 중복 시도를 피하고 REFRESH 재동기화 결과와 timestamp 갱신이 같은 흐름에서 완료된다.

## Testing Strategy
- Domain tests: 없음.
- Application tests: `PortalSyncService` 단위 테스트로 최초 LINK와 병합 후 REFRESH 전환을 검증한다.
- Integration/API tests: 외부 계약 변경이 없어 생략한다.
- Additional commands: `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests`, `./gradlew :app:api:test` 또는 이 저장소의 사용 가능한 전체 테스트 명령.

## Rollout Considerations
- Backward compatibility: 콜백/API 계약 변경이 없고 신규 최초 LINK 분기는 유지된다.
- Observability / metrics: 기존 `portal.sync.done`, `portal.refresh.done`, postprocess success/fail 로그를 그대로 사용한다.
- Feature flags / toggles: 필요 없음.
