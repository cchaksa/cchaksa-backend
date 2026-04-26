# Tasks

## Checklist
- [x] Spec bundle 작성
- [x] Maintenance application tests 작성
- [x] Lambda routing tests 작성
- [x] Maintenance application handler 구현
- [x] Lambda handler direct invoke 분기 구현
- [x] 내부 `@Scheduled` 의존 제거
- [x] Documentation updated

## Test / Build Log
| Step | Command | Result | Date |
|------|---------|--------|------|
| 1 | `./gradlew test --tests "com.chukchuk.haksa.application.maintenance.*" --tests "com.chukchuk.haksa.application.portal.ScrapeJobStaleReconcilerUnitTests" --tests "com.chukchuk.haksa.domain.auth.service.RefreshTokenServiceUnitTests" --tests "com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest"` | Pass | 2026-04-26 |
| 2 | `./gradlew test --tests "com.chukchuk.haksa.global.config.SchedulingConfigTests" --tests "com.chukchuk.haksa.application.maintenance.*" --tests "com.chukchuk.haksa.application.portal.ScrapeJobStaleReconcilerUnitTests" --tests "com.chukchuk.haksa.domain.auth.service.RefreshTokenServiceUnitTests" --tests "com.chukchuk.haksa.global.lambda.StreamLambdaHandlerTest"` | Pass | 2026-04-26 |
| 3 | `./gradlew test` | Pass | 2026-04-26 |
| 4 | `./gradlew build` | Pass | 2026-04-26 |

## Notes
- Observation: 이번 작업은 HTTP endpoint 추가 없이 EventBridge Scheduler direct Lambda invoke만 지원한다.
