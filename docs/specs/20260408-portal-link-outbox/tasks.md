# Tasks

## Checklist
- [x] Domain tests written (ScrapeJob worker 결과 기록 메서드)
- [x] Application layer updated (ScrapeResultCallbackService, PortalCallbackPostProcessor, PortalSyncService)
- [x] Infrastructure layer updated (UserPortalConnectionRepository, StudentRepository)
- [x] Global/config reviewed (추가 변경 없음 확인)
- [x] API/controller updated (변경 없음, 상태 응답 검증)
- [x] Documentation updated (spec/clarify/plan/tasks)

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | Success | 2026-04-08 |
| 2 | `./gradlew :test --tests "*PortalLink*"` | Covered by #1 | 2026-04-08 |
| 3 | `./gradlew test` | Success | 2026-04-09 |
| 4 | `./gradlew test` | Success | 2026-04-09 |
| 5 | `./gradlew test` | Success | 2026-04-09 |
| 6 | `./gradlew test` | Success | 2026-04-09 |
| 7 | `./gradlew test` | Success | 2026-04-09 |
| 8 | `./gradlew check --stacktrace --no-daemon` | Success | 2026-04-09 |

## Notes
- Observation: develop-shadow 는 scheduler disabled 상태라 afterCommit 비동기 경로 외에는 재시도 없음, executor 기반 재시도 필요.
- TODO: Student duplicate 정리 스크립트 필요 여부 판단 후 clarify #3 에 기록.
