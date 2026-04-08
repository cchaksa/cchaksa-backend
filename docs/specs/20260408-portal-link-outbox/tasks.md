# Tasks

## Checklist
- [ ] Domain tests written
- [x] Application layer updated
- [ ] Infrastructure layer updated
- [x] Global/config reviewed
- [ ] API/controller updated
- [x] Documentation updated (spec/clarify/plan/tasks)

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test` | Success | 2026-04-08 |
| 2 | `./gradlew :test --tests "*PortalLink*"` | Covered by #1 | 2026-04-08 |

## Notes
- Observation: develop-shadow 는 scheduler disabled 상태라 afterCommit 비동기 경로 외에는 재시도 없음, executor 기반 재시도 필요.
