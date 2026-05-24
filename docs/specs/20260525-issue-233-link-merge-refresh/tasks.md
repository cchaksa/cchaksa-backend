# Tasks Template

## Checklist
- [ ] Application regression test written first
- [ ] RED failure confirmed
- [ ] Application layer updated
- [ ] Infrastructure layer reviewed
- [ ] Global/config reviewed
- [ ] API/controller reviewed
- [ ] Documentation updated

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests` | Failed as expected. `syncWithPortal_usesRefreshFlowWhenMergeMakesUserConnected` failed because initial portal setup was called. | 2026-05-25 |
| 2 | `./gradlew test --tests com.chukchuk.haksa.application.portal.PortalSyncServiceTests` | Passed after fix. | 2026-05-25 |
| 3 | `./gradlew test` | Passed. | 2026-05-25 |

## Notes
- Observation: 실패 로그는 스크래퍼/S3 수신 이후 콜백 후처리에서 `portal.init.skipped reason=already_connected`로 발생했다.
- Observation: `syncWithPortal`에서 병합 후 `portalConnected=true`인 active user만 REFRESH 재동기화 경로로 전환했다.
